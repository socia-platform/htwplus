package controllers;

import java.util.Date;
import java.util.List;

import controllers.Navigation.Level;
import models.Account;
import models.Group;
import models.Notification;
import models.Notification.NotificationType;
import models.Post;
import play.Play;
import play.api.mvc.Call;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Post.view;

@Security.Authenticated(Secured.class)
public class PostController extends BaseController {
	
	static Form<Post> postForm = Form.form(Post.class);
	static final int PAGE = 1;
	
	public static Result view (Long id) {
		Post post = Post.findById(id);
		
		if(post == null){
			return redirect(routes.Application.error());
		}
		
		if(post.parent != null) {
			return redirect(routes.Application.error());
		}
		
		if(!Secured.viewPost(post)) {
			return redirect(routes.Application.index());
		}
		
		if(post.belongsToGroup()) {
			Navigation.set(Level.GROUPS, "Post", post.group.title, routes.GroupController.view(post.group.id, PAGE));
		}
		
		if(post.belongsToAccount()) {
			Navigation.set(Level.FRIENDS, "Post", post.account.name, routes.ProfileController.stream(post.account.id, PAGE));
		}
		
		return ok(view.render(post, postForm));
	}
	
	/**
	 * @author Iven
	 * @param anyId - can be a accountId or groupId
	 * @param target - define target stream: profile-stream, group-stream
	 * @return
	 */
	public static Result addPost(Long anyId, String target) {
		Account account = Component.currentAccount();
		Form<Post> filledForm = postForm.bindFromRequest();
		
		if(target.equals(Post.GROUP)) {
			Group group = Group.findById(anyId);
			if (Secured.isMemberOfGroup(group, account)) {
				if (filledForm.hasErrors()) {
					flash("error", "Jo, fast. Probiere es noch einmal mit Inhalt ;-)");
				} else {
					Post p = filledForm.get();
					p.owner = Component.currentAccount();
					p.group = group;
					p.create();
					Notification.newGroupNotification(NotificationType.GROUP_NEW_POST, group, account);
				}
			} else {
				flash("info","Bitte tritt der Gruppe erst bei.");
			}
			return redirect(routes.GroupController.view(group.id, PAGE));
		}
		
		if(target.equals(Post.PROFILE)) {
			Account profile = Account.findById(anyId);
			if(Secured.isFriend(profile) || profile.equals(account) || Secured.isAdmin()){
				if (filledForm.hasErrors()) {
					flash("error", "Jo, fast. Probiere es noch einmal mit Inhalt ;-)");
				} else {
					Post p = filledForm.get();
					p.account = profile;
					p.owner = account;
					p.create();
					if(!account.equals(profile)){
						Notification.newNotification(NotificationType.PROFILE_NEW_POST, account.id, profile);
					}
				}
				return redirect(routes.ProfileController.stream(anyId, PAGE));
			}
			flash("info","Du kannst nur Freunden auf den Stream schreiben!");
			return redirect(routes.ProfileController.stream(anyId, PAGE));
		}
		
		if(target.equals(Post.STREAM)) {
			Account profile = Account.findById(anyId);
			if(profile.equals(account)){
				if (filledForm.hasErrors()) {
					flash("error", "Jo, fast. Probiere es noch einmal mit Inhalt ;-)");
				} else {
					Post p = filledForm.get();
					p.account = profile;
					p.owner = account;
					p.create();
				}
				return redirect(routes.Application.stream(PAGE));
			}
			flash("info","Du kannst nur dir oder Freunden auf den Stream schreiben!");
			return redirect(routes.Application.stream(PAGE));
		}
		return redirect(routes.Application.index());
	}
	
	@Transactional
	public static Result addComment(long postId) {
		Post parent = Post.findById(postId);
		Account account = Component.currentAccount();
		
		if(!Secured.addComment(parent)){
			return badRequest();
		}
		
		Form<Post> filledForm = postForm.bindFromRequest();
		if (filledForm.hasErrors()) {
			return badRequest();
		} else {
			Post post = filledForm.get();
			post.owner = account;
			post.parent = parent;
			post.create();
			// update parent to move it to the top
			parent.update();
			
			if(parent.belongsToGroup()) {
				Notification.newPostNotification(NotificationType.POST_GROUP_NEW_COMMENT, parent, account);
			}
			if(parent.belongsToAccount()) {
				if(!account.equals(parent.owner) && !parent.account.equals(parent.owner) ) {
					Notification.newNotification(NotificationType.POST_PROFILE_NEW_COMMENT, parent.id, parent.owner);
				}	
				if(!account.equals(parent.account)) {
					Notification.newNotification(NotificationType.POST_MY_PROFILE_NEW_COMMENT, parent.id, parent.account);
				}				
			}
			
			return ok(views.html.snippets.postComment.render(post));
		}
	}
	

	
	
	@Transactional
	public static List<Post> getCommentsForPostInGroup(Long id) {
		int max = Integer.parseInt(Play.application().configuration().getString("htwplus.comments.init"));
		int count = Post.countCommentsForPost(id);
		if(count <= max){
			return Post.getCommentsForPost(id, 0, count);
		} else {
			return Post.getCommentsForPost(id, count-max, count);
		}
	}
	
	@Transactional
	public static Result getOlderComments(Long id, Integer current) {
		Post parent = Post.findById(id);
		
		if(!Secured.viewComments(parent)){
			return badRequest();
		}
		
		String result = "";
		//int max = Integer.parseInt(Play.application().configuration().getString("htwplus.comments.init"));
		int max = current;
		int count = Post.countCommentsForPost(id);
		List<Post> comments = null;
		if(count <= max){
			return ok(result);	
		} else {
			comments = Post.getCommentsForPost(id, 0, count-max);
			for (Post post : comments) {
				result = result.concat(views.html.snippets.postComment.render(post).toString());
			}
			return ok(result);	
		}
	}
	
	@Transactional
	public static Result deletePost(final Long postId) {
		final Post post = Post.findById(postId);
		// verify redirect after deletion
		Call routesTo = null;
		if(post.group != null){
			routesTo = routes.GroupController.view(post.group.id, PAGE);
		}
		else if(post.account != null){
			routesTo = routes.Application.index();
		}
		else if(post.parent != null)
		{
			if(post.parent.group != null){
				routesTo = routes.GroupController.view(post.parent.group.id, PAGE);
			}else if(post.parent.account != null) {
				routesTo = routes.Application.index();
			}
		}
		final Account account = Component.currentAccount();
		if (Secured.isAllowedToDeletePost(post, account)) {
			post.delete();
			flash("success", "Gelöscht!");
		} else {
			flash("error", "Konnte nicht gelöscht werden!");
		}

		return redirect(routesTo);
	}
	
}
