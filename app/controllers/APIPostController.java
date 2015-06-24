package controllers;

import models.Post;
import models.enums.CustomContentType;
import net.hamnaberg.json.*;
import net.hamnaberg.json.Error;
import play.mvc.Result;
import util.JsonCollectionUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by richard on 17.06.15.
 */
public class APIPostController extends BaseController {

    public static Result post() {
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

    public static Result get(final Long id) {
        if(request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            URI uri = URI.create(request().host() + request().path());
            Error error = Error.EMPTY;
            List<Item> items = null;

            if (id == 0) { // http GET .../users
                items = Post.allWithoutAdmin()
                        .stream()
                        .map(a -> Item.create(URI.create(uri.toString() + "/" + a.id), a.getProperties()))
                        .collect(Collectors.toList());
            } else { // http GET .../users/:id
                Post post = Post.findById(id);
                items = new ArrayList<Item>();
                if (post == null) {
                    error = Error.create("Post not found", "404", "The requeseted post does not seem to exist.");
                } else {
                    items.add(Item.create(uri, post.getProperties()));
                }
            }

            Collection collection = Collection.create(
                    uri,
                    new ArrayList<Link>(),
                    items,
                    new ArrayList<Query>(),
                    Template.create(),
                    error
            );

            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            return ok(collection.asJson());
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, "Only accepting Accept header: " + CustomContentType.JSON_COLLECTION.getIdentifier());
        }
    }
}
