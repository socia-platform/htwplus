package controllers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Account;
import models.Group;
import models.Post;
import play.Logger;
import play.Play;
import play.Routes;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import views.html.error;
import views.html.help;
import views.html.searchresult;
import views.html.stream;
import views.html.feedback;
import controllers.Navigation.Level;
import models.services.AvatarService;


@Transactional
public class Application extends BaseController {
	
	static Form<Post> postForm = Form.form(Post.class);
	static final int LIMIT = Integer.parseInt(Play.application().configuration().getString("htwplus.post.limit"));
	static final int SEARCH_LIMIT = Integer.parseInt(Play.application().configuration().getString("htwplus.search.limit"));
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
		AvatarService a = AvatarService.getInstance();
		Logger.info(a.test);
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
	
	@Security.Authenticated(Secured.class)
	public static Result search(){
		List<Group> groupResults = null;
		List<Group> courseResults = null;
		List<Account> accResults = null;
		String keyword = "";
		final Set<Map.Entry<String, String[]>> entries = request()
				.queryString().entrySet();
		for (Map.Entry<String, String[]> entry : entries) {
			if (entry.getKey().equals("keyword")) {
				keyword = entry.getValue()[0];
				Logger.info("Keyword: " +keyword.isEmpty());
				Navigation.set("Suchergebnisse");
				courseResults = Group.courseSearch(keyword, SEARCH_LIMIT, 0);
				groupResults = Group.groupSearch(keyword, SEARCH_LIMIT, 0);
				accResults = Account.accountSearch(keyword, SEARCH_LIMIT, 0);
			}

		}
		return ok(searchresult.render(groupResults, courseResults, accResults, keyword,Group.countCourseSearch(keyword), Group.countGroupSearch(keyword), Account.countAccountSearch(keyword),SEARCH_LIMIT, 0));
	}
	
	@Security.Authenticated(Secured.class)
	public static Result searchForCourses(final String keyword, int page){
		Navigation.set("Suchergebnisse für Kurse");
		List<Group> courses = Group.courseSearch(keyword, SEARCH_LIMIT, page);
		return ok(searchresult.render(null, courses, null, keyword, Group.countCourseSearch(keyword), 0, 0, SEARCH_LIMIT, page));
	}

	@Security.Authenticated(Secured.class)
	public static Result searchForGroups(final String keyword, int page){
		Navigation.set("Suchergebnisse für Gruppen");
		Logger.info("Keyword: " +keyword);
		List<Group> groups = Group.groupSearch(keyword, SEARCH_LIMIT, page);
		return ok(searchresult.render(groups, null, null, keyword, 0, Group.countGroupSearch(keyword), 0, SEARCH_LIMIT, page));
	}
	
	@Security.Authenticated(Secured.class)
	public static Result searchForAccounts(final String keyword, int page){
		Navigation.set("Suchergebnisse für Personen");
		List<Account> accounts = Account.accountSearch(keyword, SEARCH_LIMIT, page);
		return ok(searchresult.render(null, null, accounts, keyword, 0, 0, Account.countAccountSearch(keyword), SEARCH_LIMIT, page));
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
			account = Account.findByEmail("admin@htwplus.de");
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
