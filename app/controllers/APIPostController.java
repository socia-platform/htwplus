package controllers;

import com.google.common.collect.Lists;
import models.Post;
import models.enums.CustomContentType;
import net.hamnaberg.json.Collection;
import play.mvc.Result;
import play.mvc.Security;
import util.JsonCollectionUtil;

/**
 * Created by richard on 17.06.15.
 */
public class APIPostController extends BaseController {

    public static Result post() {
        if (JsonCollectionUtil.hasJsonCollection(request())) {
            Collection jcol = JsonCollectionUtil.getJsonCollection(request());
            jcol = Post.validatePost(jcol);
            if (!jcol.hasError()) {
                final Post post = new Post(jcol);
                post.create();
                return ok();
            } else {
                return badRequest(jcol.asJson());
            }
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, "Only accepting Accept header: " + CustomContentType.JSON_COLLECTION.getIdentifier());
        }
    }

    /*@Transactional
    @Security.Authenticated(SecuredWithToken.class)*/
    public static Result get(final Long id) {
        if(request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            Collection collection;
            if (id >= 0) { // http GET .../users/:id
                Post post = Post.findById(id);

                if (post == null) {
                    collection = JsonCollectionUtil.getErrorCollection(Post.class,
                            "Post not found", "404", "The requested post does not seem to exist.");
                } else {
                    collection = JsonCollectionUtil.getRequestedCollection(Post.class, Lists.newArrayList(post));
                }
            } else { // http GET ...users
                collection = JsonCollectionUtil.getRequestedCollection(Post.class, Post.allWithoutAdmin()); // Get all users from DB and limit them
//                collection = JsonCollectionUtil.getRequestedCollection(Post.class, Post::some, Error.EMPTY);           // Better get only limited users from DB
            }

            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            return ok(collection.asJson());
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, "Only accepting Accept header: " + CustomContentType.JSON_COLLECTION.getIdentifier());
        }
    }
}
