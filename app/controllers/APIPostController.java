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
            Account account = Account.findById(jcol.getFirstItem().get().getData().propertyByName("account_id").);
            final Post post = new Post();
            post.account = account;
            post.owner = account;
            post.content = jcol.asJson().get("items").get(0).get("data").get(0).get("content").toString();
            post.create();
            return ok();
        }
        else {
            return internalServerError();
        }
    }
}
