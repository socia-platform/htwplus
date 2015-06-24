package controllers;

import models.Account;
import models.Post;
import net.hamnaberg.json.Collection;
import play.mvc.Result;
import util.JsonCollectionUtil;

/**
 * Created by richard on 17.06.15.
 */
public class APIPostController extends BaseController {

    public static Result addPost() {
        if (JsonCollectionUtil.hasJsonCollection(request()))
        {
            Collection jcol = JsonCollectionUtil.getJsonCollection(request());
            jcol = Post.validatePost(jcol);
            if (!jcol.hasError()) {
                final Post post = new Post(jcol);
                post.create();
                return ok();
            } else {
                return badRequest(jcol.asJson());
            }
        }
        else {
            return internalServerError();
        }
    }
}
