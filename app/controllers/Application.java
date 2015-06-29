package controllers;

import com.typesafe.config.ConfigFactory;
import controllers.Navigation.Level;
import models.Account;
import models.Group;
import models.Post;
import models.services.ElasticsearchService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import play.Logger;
import play.Routes;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import views.html.*;
import views.html.snippets.streamRaw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;


@Transactional
public class Application extends BaseController {
	
	static Form<Post> postForm = Form.form(Post.class);
	static final int LIMIT = ConfigFactory.load().getInt("htwplus.post.limit");
	static final int PAGE = 1;

	
	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");

		return ok(
                Routes.javascriptRouter("jsRoutes",
				    controllers.routes.javascript.GroupController.create(),
				    controllers.routes.javascript.GroupController.update()
				)
        );
	}

	@Security.Authenticated(Secured.class)
	public static Result index() {
		Navigation.set(Level.STREAM, "Alles");
		Account currentAccount = Component.currentAccount();
		return ok(stream.render(currentAccount,Post.getStream(currentAccount, LIMIT, PAGE),postForm,Post.countStream(currentAccount, ""), LIMIT, PAGE, "all"));
	}
	
	public static Result help() {
		Navigation.set(Level.HELP);
		return ok(help.render());
	}
	
	@Security.Authenticated(Secured.class)
	public static Result stream(String filter, int page, boolean raw) {
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
                Navigation.set(Level.STREAM, "Favoriten");
                break;
            default:
                Navigation.set(Level.STREAM, "Alles");
        }
		Account currentAccount = Component.currentAccount();

        if(raw) {
            return ok(streamRaw.render(Post.getFilteredStream(currentAccount, LIMIT, page, filter), postForm, Post.countStream(currentAccount, filter), LIMIT, page, filter));
        } else {
            return ok(stream.render(currentAccount, Post.getFilteredStream(currentAccount, LIMIT, page, filter), postForm, Post.countStream(currentAccount, filter), LIMIT, page, filter));
        }
	}

    public static Result searchSuggestions(String query) throws ExecutionException, InterruptedException {
        SearchResponse response = ElasticsearchService.doSearch("searchSuggestions", query, "all", 1,  Component.currentAccount().id.toString(), asList("name","title"), asList("user.friends", "group.member"));
        return ok(response.toString());
    }

    public static Result searchHome() {
        Navigation.set(Level.SEARCH);
        return ok(search.render());
    }
	
	@Security.Authenticated(Secured.class)
	public static Result search(int page) throws ExecutionException, InterruptedException {
        Navigation.set(Level.SEARCH);
        Account currentAccount = Component.currentAccount();
        String keyword = Form.form().bindFromRequest().field("keyword").value();
        String mode = Form.form().bindFromRequest().field("mode").value();

        if (keyword == null) {
            flash("info","Nach was suchst du?");
            return redirect(routes.Application.searchHome());
        }

        if (mode == null) mode = "all";

        Pattern pt = Pattern.compile("[^ a-zA-Z0-9\u00C0-\u00FF]");
        Matcher match= pt.matcher(keyword);
        while(match.find())
        {
            String s = match.group();
            keyword=keyword.replaceAll("\\"+s, "");
            flash("info","Dein Suchwort enthielt ungültige Zeichen, die für die Suche entfernt wurden!");
        }

        List<Object> resultList = new ArrayList<>();

        SearchResponse response;
        long userCount = 0;
        long groupCount = 0;
        long postCount = 0;

        try {
            response = ElasticsearchService.doSearch("search", keyword.toLowerCase(), mode, page, currentAccount.id.toString(), asList("name", "title", "content"), asList("user.friends", "user.owner", "group.member", "group.owner", "post.owner", "post.viewable"));
        } catch (NoNodeAvailableException nna) {
            flash("error", "Leider steht die Suche zur Zeit nicht zur Verfügung!");
            return ok(search.render());
        }

        /**
         * Iterate over response and add each searchHit to one list.
         * Pay attention to view rights for post.content.
         */
        for (SearchHit searchHit : response.getHits().getHits()) {
            switch (searchHit.type()) {
                case "user":
                    resultList.add(Account.findById(Long.parseLong(searchHit.getId())));
                    break;
                case "post":
                    Post post = Post.findById(Long.parseLong(searchHit.getId()));
                    String searchContent = post.content;
                    if(!searchHit.getHighlightFields().isEmpty())
                        searchContent = searchHit.getHighlightFields().get("content").getFragments()[0].string();
                    post.searchContent = StringEscapeUtils.escapeHtml4(searchContent)
                            .replace("[startStrong]","<strong>")
                            .replace("[endStrong]","</strong>");
                    resultList.add(post);
                    break;
                case "group":
                    resultList.add(Group.findById(Long.parseLong(searchHit.getId())));
                    break;
                default: Logger.info("no matching case for ID: "+searchHit.getId());
            }
        }

        Terms terms = response.getAggregations().get("types");
        Collection<Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            switch (bucket.getKey()) {
                case "user":
                    userCount = bucket.getDocCount();
                    break;
                case "group":
                    groupCount = bucket.getDocCount();
                    break;
                case "post":
                    postCount = bucket.getDocCount();
                    break;
            }
        }

        return ok(views.html.searchresult.render(keyword, mode, page, LIMIT, resultList, response.getTookInMillis(), userCount+groupCount+postCount, userCount, groupCount, postCount));
	}

	public static Result error() {
		Navigation.set("404");
		return ok(error.render());
	}
	
	public static Result feedback() {
        Navigation.set("Feedback");
        return ok(feedback.render(postForm));
    }
	
	public static Result addFeedback() {
		
		Account account = Component.currentAccount();
		Group group = Group.findByTitle("HTWplus Feedback");
		
		// Guest case
		if(account == null) {
			account = Account.findByEmail(play.Play.application().configuration().getString("htwplus.admin.mail"));
		}
		
		Form<Post> filledForm = postForm.bindFromRequest();
		if (filledForm.hasErrors()) {
			flash("error", "Jo, fast. Probiere es noch einmal mit Inhalt ;-)");
			return redirect(controllers.routes.Application.feedback());
		} else {
			Post p = filledForm.get();
			p.owner = account;
            p.group = group;
            p.create();
            flash("success","Vielen Dank für Dein Feedback!");
		}

		return redirect(controllers.routes.Application.index());
	}

    public static Result imprint() {
        Navigation.set("Impressum");
        return ok(imprint.render());
    }

    public static Result privacy() {
        Navigation.set("Datenschutzerklärung");
        return ok(privacy.render());
    }

	public static Result defaultRoute(String path) {
		Logger.info(path+" nicht gefunden");
		return redirect(controllers.routes.Application.index());
	}

}
