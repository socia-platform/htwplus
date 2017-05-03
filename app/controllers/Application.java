package controllers;

import controllers.Navigation.Level;
import managers.AccountManager;
import managers.GroupManager;
import managers.PostManager;
import models.Account;
import models.Group;
import models.Post;
import models.services.ElasticsearchResponse;
import models.services.ElasticsearchService;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import play.Configuration;
import play.Logger;
import play.api.i18n.Lang;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.mvc.Result;
import play.mvc.Security;
import play.routing.JavaScriptReverseRouter;
import views.html.*;
import views.html.snippets.streamRaw;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;


@Transactional
public class Application extends BaseController {

    ElasticsearchService elasticsearchService;
    ElasticsearchResponse elasticsearchResponse;
    GroupManager groupManager;
    PostManager postManager;
    AccountManager accountManager;
    Configuration configuration;
    FormFactory formFactory;
    MessagesApi messagesApi;

    @Inject
    public Application(ElasticsearchService elasticsearchService,
                       ElasticsearchResponse elasticsearchResponse,
                       GroupManager groupManager,
                       PostManager postManager,
                       AccountManager accountManager,
                       Configuration configuration,
                       FormFactory formFactory,
                       MessagesApi messagesApi) {
        this.elasticsearchService = elasticsearchService;
        this.elasticsearchResponse = elasticsearchResponse;
        this.groupManager = groupManager;
        this. postManager = postManager;
        this.accountManager = accountManager;
        this.configuration = configuration;
        this.messagesApi = messagesApi;
        this.formFactory = formFactory;
        this.postForm = formFactory.form(Post.class);
        this.limit = configuration.getInt("htwplus.post.limit");
    }

    Form<Post> postForm;
    int limit;
    static final int PAGE = 1;

    public Result javascriptRoutes() {
        return ok(
                JavaScriptReverseRouter.create("jsRoutes",
                        controllers.routes.javascript.GroupController.create(),
                        controllers.routes.javascript.GroupController.update()
                )
        ).as("text/javascript");
    }

    @Security.Authenticated(Secured.class)
    public Result index() {
        Navigation.set(Level.STREAM, "Alles");
        Account currentAccount = Component.currentAccount();
        return ok(stream.render(currentAccount, postManager.getStream(currentAccount, limit, PAGE), postForm, postManager.countStream(currentAccount, ""), limit, PAGE, "all"));
    }

    public Result help() {
        Navigation.set(Level.HELP);
        return ok(help.render());
    }

    @Security.Authenticated(Secured.class)
    public Result stream(String filter, int page, boolean raw) {
        Account currentAccount = Component.currentAccount();

        // filter must be set for pagination
        // cant do it in routes, routes.Application.stream(filter, 1).toString would truncate it
        if(StringUtils.isEmpty(filter)) {
            filter = "all";
        }

        switch (filter) {
            case "account":
                Navigation.set(Level.STREAM, "Eigene Posts");
                break;
            case "group":
                Navigation.set(Level.STREAM, "Gruppen");
                break;
            case "contact":
                Navigation.set(Level.STREAM, "Kontakte");
                break;
            case "bookmark":
                Navigation.set(Level.STREAM, "Gemerkte Posts");
                break;
            default:
                Navigation.set(Level.STREAM, "Alles");
                filter = "all";
        }

        if (raw) {
            return ok(streamRaw.render(postManager.getFilteredStream(currentAccount, limit, page, filter), postForm, postManager.countStream(currentAccount, filter), limit, page, filter));
        } else {
            return ok(stream.render(currentAccount, postManager.getFilteredStream(currentAccount, limit, page, filter), postForm, postManager.countStream(currentAccount, filter), limit, page, filter));
        }
    }

    @Security.Authenticated(Secured.class)
    public Result searchSuggestions(String query) throws ExecutionException, InterruptedException {
        Account currentAccount = Component.currentAccount();
        if (currentAccount == null) {
            return forbidden();
        }
        SearchResponse response = elasticsearchService.doSearch("searchSuggestions", query, "all", null, 1, currentAccount.id.toString(), asList("name", "title", "filename"), asList("friends", "member", "viewable"));
        return ok(response.toString());
    }

    @Security.Authenticated(Secured.class)
    public Result searchHome() {
        Navigation.set(Level.SEARCH);
        return ok(views.html.Search.search.render());
    }

    @Security.Authenticated(Secured.class)
    public Result search(int page) throws ExecutionException, InterruptedException {

        Account currentAccount = Component.currentAccount();
        String keyword = formFactory.form().bindFromRequest().field("keyword").value();
        String mode = formFactory.form().bindFromRequest().field("mode").value();
        String studycourseParam = formFactory.form().bindFromRequest().field("studycourse").value();
        String degreeParam = formFactory.form().bindFromRequest().field("degree").value();
        String semesterParam = formFactory.form().bindFromRequest().field("semester").value();
        String roleParam = formFactory.form().bindFromRequest().field("role").value();
        String grouptypeParam = formFactory.form().bindFromRequest().field("grouptype").value();

        HashMap<String, String[]> facets = new HashMap<>();
        facets.put("studycourse", buildUserFacetList(studycourseParam));
        facets.put("degree", buildUserFacetList(degreeParam));
        facets.put("semester", buildUserFacetList(semesterParam));
        facets.put("role", buildUserFacetList(roleParam));
        facets.put("grouptype", buildUserFacetList(grouptypeParam));


        if (keyword == null) {
            flash("info", "Nach was suchst du?");
            return redirect(controllers.routes.Application.searchHome());
        }

        if (mode == null) mode = "all";

        Pattern pt = Pattern.compile("[^ a-zA-Z0-9\u00C0-\u00FF]");
        Matcher match = pt.matcher(keyword);
        while (match.find()) {
            String s = match.group();
            keyword = keyword.replaceAll("\\" + s, "");
            flash("info", "Dein Suchwort enthielt ungültige Zeichen, die für die Suche entfernt wurden!");
        }

        SearchResponse response;

        try {
            response = elasticsearchService.doSearch("search", keyword.toLowerCase(), mode, facets, page, currentAccount.id.toString(), asList("name", "title", "content", "filename"), asList("friends", "owner", "member", "viewable"));
            elasticsearchResponse.create(response, keyword, mode);
        } catch (NoNodeAvailableException nna) {
            flash("error", "Leider steht die Suche zur Zeit nicht zur Verfügung!");
            return ok(views.html.Search.search.render());
        }

        Navigation.set(Level.SEARCH, elasticsearchResponse.getDocumentCount() + " Ergebnisse zu \""+ keyword +"\"");
        if (!mode.isEmpty() && !mode.equals("all")) {
            Navigation.set(Level.SEARCH, elasticsearchResponse.getDocumentCount() + " Ergebnisse zu \""+ keyword +"\"", messagesApi.get(Lang.defaultLang(), "search."+mode), null);
        }

        return ok(views.html.Search.searchresult.render(
                page,
                limit,
                elasticsearchResponse));
    }

    public Result error() {
        Navigation.set("404");
        return ok(error.render());
    }

    @Security.Authenticated(Secured.class)
    public Result feedback() {
        final String feedbackGroup = configuration.getString("htwplus.feedback.group");
        Group feedback = groupManager.findByTitle(feedbackGroup);
        if (feedback != null) {
            return redirect(controllers.routes.GroupController.view(feedback.id));
        }
        return notFound();
    }

    public Result imprint() {
        Navigation.set("Impressum");
        return ok(imprint.render());
    }

    public Result privacy() {
        Navigation.set("Datenschutzerklärung");
        return ok(privacy.render());
    }

    public Result defaultRoute(String path) {
        Logger.info(path + " nicht gefunden");
        return redirect(controllers.routes.Application.index());
    }

    private String[] buildUserFacetList(String parameter) {
        if (parameter != null) {
            return parameter.split(",");
        }
        return new String[0];
    }

}
