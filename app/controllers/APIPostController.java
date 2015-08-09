package controllers;

import models.Post;
import models.enums.CustomContentType;
import net.hamnaberg.json.Collection;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import util.JsonCollectionUtil;

/**
 * Created by richard on 17.06.15.
 */
@Transactional
@Security.Authenticated(SecuredWithToken.class)
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
                return badRequest(JsonCollectionUtil.addTemplate(Post.class, jcol).toString());
            }
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, CustomContentType.JSON_COLLECTION.getAcceptHeaderMessage());
        }
    }

    public static Result get(final Long id) {
        if(request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            return ok(JsonCollectionUtil.getRequestedCollectionString(Post.class, id, "Post"));
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, CustomContentType.JSON_COLLECTION.getAcceptHeaderMessage());
        }
    }
}
