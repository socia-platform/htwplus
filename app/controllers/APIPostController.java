package controllers;

import models.Post;
import net.hamnaberg.json.Collection;
import play.mvc.Result;
import util.JsonCollectionUtil;

/**
 * Created by richard on 17.06.15.
 */
public class APIPostController extends BaseController {

    public static Result addPost() {
        /*if (JsonCollectionUtil.hasJsonCollection(request()))
        {
            Collection jcol = JsonCollectionUtil.getJsonCollection(request());
            final Post post = new Post();
            post.account = profile;
            post.owner = account;
            post.content = jcol.asJson().get("items").get(0).get("data").get(0).get("content").toString();
            post.create();

            return ok();
        }
        else {
            return internalServerError();
        }*/
        return notFound("Action not implemented yet");
    }
}
