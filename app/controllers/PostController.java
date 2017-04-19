package controllers;

import controllers.Navigation.Level;
import managers.AccountManager;
import managers.GroupManager;
import managers.PostBookmarkManager;
import managers.PostManager;
import models.Account;
import models.Group;
import models.Post;
import models.PostBookmark;
import models.services.NotificationService;
import play.Configuration;
import play.api.i18n.Lang;
import play.api.mvc.Call;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Post.view;

import javax.inject.Inject;
import java.util.List;

@Security.Authenticated(Secured.class)
public class PostController extends BaseController {

    GroupManager groupManager;
    PostManager postManager;
    PostBookmarkManager postBookmarkManager;
    AccountManager accountManager;
    Configuration configuration;
    FormFactory formFactory;
    NotificationService notificationService;
    MessagesApi messagesApi;

    @Inject
    public PostController(GroupManager groupManager,
                          PostManager postManager,
            PostBookmarkManager postBookmarkManager,
                          AccountManager accountManager,
                          Configuration configuration,
                          FormFactory formFactory,
                          NotificationService notificationService,
                          MessagesApi messagesApi) {
        this.groupManager = groupManager;
        this.postManager = postManager;
        this.postBookmarkManager = postBookmarkManager;
        this.accountManager = accountManager;
        this.configuration = configuration;
        this.formFactory = formFactory;
        this.notificationService = notificationService;
        this.messagesApi = messagesApi;

        this.postForm = formFactory.form(Post.class);
    }

    Form<Post> postForm;
    static final int PAGE = 1;
    static final String STREAM_FILTER = "all";

    public Result view(Long id) {
        Post post = postManager.findById(id);

        if (post == null) {
            return redirect(controllers.routes.Application.error());
        }

        if (post.parent != null) {
            return redirect(controllers.routes.Application.error());
        }

        if (!Secured.viewPost(post)) {
            return redirect(controllers.routes.Application.index());
        }

        if (postManager.belongsToGroup(post)) {
            Navigation.set(Level.GROUPS, "Post", post.group.title, controllers.routes.GroupController.stream(post.group.id, PAGE, false));
        }

        if (post.belongsToAccount()) {
            Navigation.set(Level.FRIENDS, "Post", post.account.name, controllers.routes.ProfileController.stream(post.account.id, PAGE, false));
        }

        return ok(view.render(post, postForm));
    }

    /**
     * Adds a post.
     *
     * @param anyId  can be a accountId or groupId
     * @param target define target stream: profile-stream, group-stream
     * @return Result
     */
    @Transactional
    public Result addPost(Long anyId, String target) {
        Account account = Component.currentAccount();
        Form<Post> filledForm = postForm.bindFromRequest();

        if (target.equals(Post.GROUP)) {
            Group group = groupManager.findById(anyId);
            if (Secured.isMemberOfGroup(group, account)) {
                if (filledForm.hasErrors()) {
                    flash("error", messagesApi.get(Lang.defaultLang(), "post.try_with_content"));
                } else {
                    Post post = filledForm.get();
                    post.owner = Component.currentAccount();
                    post.group = group;
                    postManager.create(post);
                    notificationService.createNotification(post, Post.GROUP);
                }
            } else {
                flash("info", messagesApi.get(Lang.defaultLang(), "post.join_group_first"));
            }

            return redirect(controllers.routes.GroupController.stream(group.id, PAGE, false));
        }

        if (target.equals(Post.PROFILE)) {
            Account profile = accountManager.findById(anyId);
            if (Secured.isNotNull(profile) && (Secured.isFriend(profile) || profile.equals(account) || Secured.isAdmin())) {
                if (filledForm.hasErrors()) {
                    flash("error", messagesApi.get(Lang.defaultLang(), "post.try_with_content"));
                } else {
                    Post post = filledForm.get();
                    post.account = profile;
                    post.owner = account;
                    postManager.create(post);
                    if (!account.equals(profile)) {
                        notificationService.createNotification(post, Post.PROFILE);
                    }
                }

                return redirect(controllers.routes.ProfileController.stream(anyId, PAGE, false));
            }

            flash("info", messagesApi.get(Lang.defaultLang(), "post.post_on_stream_only"));
            return redirect(controllers.routes.ProfileController.stream(anyId, PAGE, false));
        }

        if (target.equals(Post.STREAM)) {
            Account profile = accountManager.findById(anyId);
            if (Secured.isNotNull(profile) && profile.equals(account)) {
                if (filledForm.hasErrors()) {
                    flash("error", messagesApi.get(Lang.defaultLang(), "post.try_with_content"));
                } else {
                    Post post = filledForm.get();
                    post.account = profile;
                    post.owner = account;
                    postManager.create(post);
                }
                return redirect(controllers.routes.Application.stream(STREAM_FILTER, PAGE, false));
            }

            flash("info", messagesApi.get(Lang.defaultLang(), "post.post_on_stream_only"));
            return redirect(controllers.routes.Application.stream(STREAM_FILTER, PAGE, false));
        }

        return redirect(controllers.routes.Application.index());
    }

    @Transactional
    public Result addComment(long postId) {
        final Post parent = postManager.findById(postId);
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
            postManager.create(post);
            // update parent to move it to the top
            postManager.update(parent);

            if (postManager.belongsToGroup(parent)) {
                // this is a comment in a group post
                notificationService.createNotification(post, Post.COMMENT_GROUP);
            }

            if (postManager.belongsToAccount(parent)) {
                if (!account.equals(parent.owner) && !parent.account.equals(parent.owner)) {
                    // this is a comment on a news stream post from another person
                    notificationService.createNotification(post, Post.COMMENT_OWN_PROFILE);
                } else if (!account.equals(parent.account)) {
                    // this is a comment on a foreign news stream post
                    notificationService.createNotification(post, Post.COMMENT_PROFILE);
                }
            }

            return ok(views.html.snippets.postComment.render(post));
        }
    }

    public Result getEditForm(Long postId) {
        Post post = postManager.findById(postId);
        Account account = Component.currentAccount();

        if (!Secured.isPostStillEditable(post, account)) {
            return badRequest();
        }

        return ok(views.html.snippets.editPost.render(post, postForm.fill(post)));
    }

    @Transactional
    public Result updatePost(Long postId) {
        Post post = postManager.findById(postId);
        Account account = Component.currentAccount();

        if (!Secured.isPostStillEditableWithTolerance(post, account)) {
            return badRequest();
        } else {
            Form<Post> filledForm = postForm.bindFromRequest();

            if (filledForm.hasErrors()) {
                return badRequest();
            } else {
                Post newPost = filledForm.get();
                post.content = newPost.content;
                postManager.create(post); // updates elasticsearch stuff
                postManager.update(post);

                return ok(post.content);
            }
        }
    }

    @Transactional
    public static List<Post> getComments(Long id, int limit) {
        //int max = Integer.parseInt(Play.application().configuration().getString("htwplus.comments.init"));
        int offset = 0;
        if (limit != 0) {
            offset = PostManager.countCommentsForPost2(id) - limit;
        }
        return PostManager.getCommentsForPost2(id, limit, offset);
    }


    @Transactional
    public Result getOlderComments(Long id, Integer current) {
        Post parent = postManager.findById(id);

        if (!Secured.viewComments(parent)) {
            return badRequest();
        }

        String result = "";

        // subtract already displayed comments
        int limit = postManager.countCommentsForPost(id) - Integer.parseInt(configuration.getString("htwplus.comments.init"));

        List<Post> comments;
        comments = postManager.getCommentsForPost(id, limit, 0);
        for (Post post : comments) {
            result = result.concat(views.html.snippets.postComment.render(post).toString());
        }
        return ok(result);
    }

    @Transactional
    public Result deletePost(Long postId) {
        Post post = postManager.findById(postId);
        Account account = Component.currentAccount();
        Call routesTo = null;

        if (Secured.isAllowedToDeletePost(post, account)) {

            //verify redirect
            if (post.group != null) {
                routesTo = controllers.routes.GroupController.stream(post.group.id, PAGE, false);
            }

            if (post.account != null) {
                routesTo = controllers.routes.Application.index();
            }

            if (post.parent != null) {
                if (post.parent.group != null) {
                    routesTo = controllers.routes.GroupController.stream(post.parent.group.id, PAGE, false);
                } else if (post.parent.account != null) {
                    routesTo = controllers.routes.Application.index();
                }
            }
            postManager.delete(post);
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
        Post post = postManager.findById(postId);
        String returnStatement = "";

        if (Secured.viewPost(post)) {
            PostBookmark possibleBookmark = postBookmarkManager.findByAccountAndPost(account, post);
            if (possibleBookmark == null) {
                postBookmarkManager.create(new PostBookmark(account, post));
                returnStatement = "setBookmark";
            } else {
                postBookmarkManager.delete(possibleBookmark);
                returnStatement = "removeBookmark";
            }
        }
        return ok(returnStatement);
    }
}
