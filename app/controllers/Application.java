package controllers;

import models.Account;
import models.Group;
import models.Post;
import models.services.ElasticsearchService;
import org.elasticsearch.search.SearchHit;
import play.Logger;
import play.Play;
import play.Routes;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import views.html.*;
import controllers.Navigation.Level;
import org.elasticsearch.action.search.SearchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import static java.util.Arrays.asList;


@Transactional
public class Application extends BaseController {
	
	static Form<Post> postForm = Form.form(Post.class);
	static final int LIMIT = Integer.parseInt(Play.application().configuration().getString("htwplus.post.limit"));
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
		Navigation.set(Level.STREAM);
		Account currentAccount = Component.currentAccount();
		return ok(stream.render(currentAccount,Post.getStream(currentAccount, LIMIT, PAGE),postForm,Post.countStream(currentAccount), LIMIT, PAGE));
	}
	
	public static Result help() {
		Navigation.set(Level.HELP);
		return ok(help.render());
	}
	
	@Security.Authenticated(Secured.class)
	public static Result stream(int page) {
		Navigation.set(Level.STREAM);
		Account currentAccount = Component.currentAccount();
		return ok(stream.render(currentAccount,Post.getStream(currentAccount, LIMIT, page),postForm,Post.countStream(currentAccount), LIMIT, page));
	}

    public static Result searchSuggestions(String query) throws ExecutionException, InterruptedException {
        SearchResponse response = ElasticsearchService.doSearch(query, 1,  Component.currentAccount().id.toString(), asList("name","title"), asList("user.friends", "group.member"));
        return ok(response.toString());
    }
	
	@Security.Authenticated(Secured.class)
	public static Result search(int page) throws ExecutionException, InterruptedException {
        Account currentAccount = Component.currentAccount();
        String keyword = Form.form().bindFromRequest().field("keyword").value();
        String mode = Form.form().bindFromRequest().field("mode").value();
        Logger.info("searching for: "+keyword+" on "+mode);

        if (keyword == null) return ok(search.render());
        if (mode == null) mode = "all";

        List<Object> resultList =new ArrayList<>();

        SearchResponse response = null;

        /**
         * Select fields for search
         */
        switch (mode) {
            case "user":
                response = ElasticsearchService.doSearch(keyword, page, currentAccount.id.toString(), asList("name"), asList("friends"));
                break;
            case "group":
                response = ElasticsearchService.doSearch(keyword, page, currentAccount.id.toString(), asList("title"), asList("members"));
                break;
            case "course":
                response = ElasticsearchService.doSearch(keyword, page, currentAccount.id.toString(), asList("title"), asList("member"));
                break;
            case "post":
                response = ElasticsearchService.doSearch(keyword, page, currentAccount.id.toString(), asList("content"), asList("post.owner"));
                break;
            default: response = ElasticsearchService.doSearch(keyword, page, currentAccount.id.toString(), asList("name", "title", "content"), asList("user.friends", "group.members", "post.owner"));
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
                    Post postParent = null;
                    // comment? check parent post
                    if (post.parent != null) {
                        postParent = post.parent;
                    }
                    if (Secured.viewPost(post) || Secured.viewPost(postParent)) {
                        post.searchContent = searchHit.getHighlightFields().get("content").getFragments()[0].string();
                        resultList.add(post);
                    }
                    break;
                case "group":
                    Group group = Group.findById(Long.parseLong(searchHit.getId()));
                    resultList.add(Group.findById(Long.parseLong(searchHit.getId())));
                    break;
                default: Logger.info("no matching case for ID: "+searchHit.getId());
            }
        }

        return ok(views.html.searchresult.render(keyword, mode, page, LIMIT, resultList, response.getTookInMillis(), response.getHits().totalHits()));
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

	public static Result defaultRoute(String path) {
		Logger.info(path+" nicht gefunden");
		return redirect(controllers.routes.Application.index());
	}

}
