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
    static final String STREAM_FILTER = "all";
	
	public Result view (Long id) {
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
			Navigation.set(Level.GROUPS, "Post", post.group.title, controllers.routes.GroupController.stream(post.group.id, PAGE));
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
    @Transactional
	public Result addPost(Long anyId, String target) {
		Account account = Component.currentAccount();
		Form<Post> filledForm = postForm.bindFromRequest();
		
		if (target.equals(Post.GROUP)) {
			Group group = Group.findById(anyId);
			if (Secured.isMemberOfGroup(group, account)) {
				if (filledForm.hasErrors()) {
					flash("error", Messages.get("post.try_with_content"));
				} else {
					final Post post = filledForm.get();
					post.owner = Component.currentAccount();
					post.group = group;
					post.create();
                    NotificationService.getInstance().createNotification(post, Post.GROUP);
				}
			} else {
				flash("info", Messages.get("post.join_group_first"));
			}
            
			return redirect(controllers.routes.GroupController.stream(group.id, PAGE));
		}
		
		if (target.equals(Post.PROFILE)) {
			Account profile = Account.findById(anyId);
			if (Secured.isNotNull(profile) && (Secured.isFriend(profile) || profile.equals(account) || Secured.isAdmin())) {
				if (filledForm.hasErrors()) {
					flash("error", Messages.get("post.try_with_content"));
				} else {
					final Post post = filledForm.get();
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
			if(Secured.isNotNull(profile) && profile.equals(account)){
				if (filledForm.hasErrors()) {
					flash("error", Messages.get("post.try_with_content"));
				} else {
					final Post post = filledForm.get();
					post.account = profile;
					post.owner = account;
					post.create();
				}
				return redirect(controllers.routes.Application.stream(STREAM_FILTER, PAGE));
			}
            
			flash("info", Messages.get("post.post_on_stream_only"));
			return redirect(controllers.routes.Application.stream(STREAM_FILTER, PAGE));
		}
        
		return redirect(controllers.routes.Application.index());
	}
	
	@Transactional
	public Result addComment(long postId) {
		final Post parent = Post.findById(postId);
		final Account account = Component.currentAccount();
		
		if (!Secured.addComment(parent)) {
			return badRequest();
		}
		
		final Form<Post> filledForm = postForm.bindFromRequest();
		if (filledForm.hasErrors()) {
			return badRequest();
		} else {
			final Post post = filledForm.get();
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
                } else if (!account.equals(parent.account)) {
                    // this is a comment on a foreign news stream post
                    NotificationService.getInstance().createNotification(post, Post.COMMENT_PROFILE);
				}				
			}
			
			return ok(views.html.snippets.postComment.render(post));
		}
	}

    public Result getEditForm(Long postId) {
        Post post = Post.findById(postId);
        Account account = Component.currentAccount();

        if(!Secured.isPostStillEditable(post, account)) {
            return badRequest();
        }

        return ok(views.html.snippets.editPost.render(post.id, postForm.fill(post)));
    }

    @Transactional
    public Result updatePost(Long postId) {
        Post post = Post.findById(postId);
        Account account = Component.currentAccount();

        if(!Secured.isPostStillEditableWithTolerance(post, account)) {
            return badRequest();
        } else {
            Form<Post> filledForm = postForm.bindFromRequest();

            if(filledForm.hasErrors()) {
                return badRequest();
            } else {
                Post newPost = filledForm.get();
                post.content = newPost.content;
                post.create(); // updates elasticsearch stuff
                post.update();

                return ok(post.content);
            }
        }
    }

	@Transactional
	public static List<Post> getComments(Long id, int limit) {
		//int max = Integer.parseInt(Play.application().configuration().getString("htwplus.comments.init"));
		int offset = 0;
		if(limit != 0){
			offset = Post.countCommentsForPost(id) - limit;
		}
		return Post.getCommentsForPost(id, limit, offset);
	}
	
	
	@Transactional
	public Result getOlderComments(Long id, Integer current) {
		Post parent = Post.findById(id);
		
		if (!Secured.viewComments(parent)) {
			return badRequest();
		}
		
		String result = "";
		
		// subtract already displayed comments
		int limit = Post.countCommentsForPost(id) - Integer.parseInt(Play.application().configuration().getString("htwplus.comments.init"));
		
		List<Post> comments;
		comments = Post.getCommentsForPost(id, limit, 0);
		for (Post post : comments) {
			result = result.concat(views.html.snippets.postComment.render(post).toString());
		}
		return ok(result);	
	}
	
	@Transactional
	public Result deletePost(Long postId) {
		Post post = Post.findById(postId);
		Account account = Component.currentAccount();
		Call routesTo = null;
		
		if (Secured.isAllowedToDeletePost(post, account)) {
			
			//verify redirect
			if (post.group != null) {
				routesTo = controllers.routes.GroupController.stream(post.group.id, PAGE);
			}
			
			if (post.account != null) {
				routesTo = controllers.routes.Application.index();
			}
			
			if (post.parent != null) {
				if (post.parent.group != null) {
					routesTo = controllers.routes.GroupController.stream(post.parent.group.id, PAGE);
				} else if (post.parent.account != null) {
					routesTo = controllers.routes.Application.index();
				}
			}
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

    public Result bookmarkPost(Long postId) {
        Account account = Component.currentAccount();
        Post post = Post.findById(postId);
        String returnStatement = "";

        if(Secured.viewPost(post)) {
            PostBookmark possibleBookmark = PostBookmark.findByAccountAndPost(account, post);
            if(possibleBookmark == null) {
                new PostBookmark(account, post).create();
                returnStatement = "setBookmark";
            } else {
                possibleBookmark.delete();
                returnStatement = "removeBookmark";
            }
        }
        return ok(returnStatement);
    }
}
