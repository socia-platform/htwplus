package controllers;

import java.util.List;

import controllers.Navigation.Level;
import models.*;
import models.services.NotificationService;
import play.Play;
import play.api.mvc.Call;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
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
			return redirect(controllers.routes.Application.error());
		}
		
		if(post.parent != null) {
			return redirect(controllers.routes.Application.error());
		}
		
		if(!Secured.viewPost(post)) {
			return redirect(controllers.routes.Application.index());
		}
		
		if(post.belongsToGroup()) {
			Navigation.set(Level.GROUPS, "Post", post.group.title, controllers.routes.GroupController.view(post.group.id, PAGE));
		}
		
		if(post.belongsToAccount()) {
			Navigation.set(Level.FRIENDS, "Post", post.account.name, controllers.routes.ProfileController.stream(post.account.id, PAGE));
		}
		
		return ok(view.render(post, postForm));
	}
	
	/**
     * Adds a post.
     *
	 * @param anyId can be a accountId or groupId
	 * @param target define target stream: profile-stream, group-stream
	 * @return Result
	 */
	public static Result addPost(Long anyId, String target) {
		Account account = Component.currentAccount();
		Form<Post> filledForm = postForm.bindFromRequest();
		
		if (target.equals(Post.GROUP)) {
			Group group = Group.findById(anyId);
			if (Secured.isMemberOfGroup(group, account)) {
				if (filledForm.hasErrors()) {
					flash("error", Messages.get("post.try_with_content"));
				} else {
					Post post = filledForm.get();
					post.owner = Component.currentAccount();
					post.group = group;
					post.create();
                    NotificationService.getInstance().createNotification(post, Post.GROUP);
				}
			} else {
				flash("info", Messages.get("post.join_group_first"));
			}
            
			return redirect(controllers.routes.GroupController.view(group.id, PAGE));
		}
		
		if (target.equals(Post.PROFILE)) {
			Account profile = Account.findById(anyId);
			if (Secured.isFriend(profile) || profile.equals(account) || Secured.isAdmin()) {
				if (filledForm.hasErrors()) {
					flash("error", Messages.get("post.try_with_content"));
				} else {
					Post post = filledForm.get();
					post.account = profile;
					post.owner = account;
					post.create();
					if (!account.equals(profile)) {
                        NotificationService.getInstance().createNotification(post, Post.PROFILE);
					}
				}

				return redirect(controllers.routes.ProfileController.stream(anyId, PAGE));
			}
            
			flash("info", Messages.get("post.post_on_stream_only"));
			return redirect(controllers.routes.ProfileController.stream(anyId, PAGE));
		}
		
		if (target.equals(Post.STREAM)) {
			Account profile = Account.findById(anyId);
			if(profile.equals(account)){
				if (filledForm.hasErrors()) {
					flash("error", Messages.get("post.try_with_content"));
				} else {
					Post post = filledForm.get();
					post.account = profile;
					post.owner = account;
					post.create();
				}
				return redirect(controllers.routes.Application.stream(PAGE));
			}
            
			flash("info", Messages.get("post.post_on_stream_only"));
			return redirect(controllers.routes.Application.stream(PAGE));
		}
        
		return redirect(controllers.routes.Application.index());
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
			
			if (parent.belongsToGroup()) {
				// this is a comment in a group post
                NotificationService.getInstance().createNotification(post, Post.COMMENT_GROUP);
			}

			if (parent.belongsToAccount()) {
				if (!account.equals(parent.owner) && !parent.account.equals(parent.owner)) {
					// this is a comment on a news stream post from another person
                    NotificationService.getInstance().createNotification(post, Post.COMMENT_OWN_PROFILE);
                }
				if (!account.equals(parent.account)) {
                    // this is a comment on a foreign news stream post
                    NotificationService.getInstance().createNotification(post, Post.COMMENT_PROFILE);
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
		List<Post> comments;
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
		if (post.group != null) {
			routesTo = controllers.routes.GroupController.view(post.group.id, PAGE);
		}
		else if (post.account != null) {
			routesTo = controllers.routes.Application.index();
		}
		else if (post.parent != null) {
			if (post.parent.group != null) {
				routesTo = controllers.routes.GroupController.view(post.parent.group.id, PAGE);
			} else if (post.parent.account != null) {
				routesTo = controllers.routes.Application.index();
			}
		}
		final Account account = Component.currentAccount();
		if (Secured.isAllowedToDeletePost(post, account)) {
			post.delete();
			flash("success", "Gelöscht!");
		} else {
			flash("error", "Konnte nicht gelöscht werden!");
		}

        if (routesTo == null) {
            routesTo = controllers.routes.Application.index();
        }

		return redirect(routesTo);
	}
}
