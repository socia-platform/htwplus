package controllers;

import models.Account;
import models.Group;
import models.Post;
import models.enums.CustomContentType;
import net.hamnaberg.json.*;
import net.hamnaberg.json.Error;
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

    /**
     * Validates the given collection and creates a post accordingly.
     * @return ok, if the post could be created, bad request with the received collection and errors or not acceptable
     * if the client does not accept Collection+JSON
     */
    public static Result post() {
        if (JsonCollectionUtil.hasJsonCollection(request())) {
            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            Collection jcol = JsonCollectionUtil.getJsonCollection(request());
            jcol = JsonCollectionUtil.checkForMissingItems(jcol, new Post());
            if (!jcol.hasError()) {
                final Post post = new Post(jcol);
                Account tokenUserAccount = Account.findById(Long.parseLong(session().get("token_user")));
                if (post.owner != null) {
                    if (post.owner.id == tokenUserAccount.id) {
                        if (post.account != null) {
                            if (post.account.id == tokenUserAccount.id) {
                                post.create();
                                return ok();
                            } else if (tokenUserAccount.friends.contains(Account.findById(tokenUserAccount.id))) {
                                post.create();
                                return ok();
                            } else {
                                return badRequestWithErrorJCol("Invalid request"
                                        , "406"
                                        , "Account id must be either your account, a friends account or null."
                                        , jcol);
                            }
                        } else if (post.group != null) {
                            if (tokenUserAccount.groupMemberships.contains(Group.findById(post.group.id))) {
                                post.create();
                                return ok();
                            } else {
                                return badRequestWithErrorJCol("Invalid request"
                                        , "406"
                                        , "You must be member of the group you want to post in."
                                        , jcol);
                            }
                        } else {
                            return badRequestWithErrorJCol("Invalid request"
                                    , "406"
                                    , "You have to specify either a valid group or account."
                                    , jcol);
                        }
                    } else {
                        return badRequestWithErrorJCol("Invalid request", "406", "You have to be owner of this post.", jcol);
                    }
                } else {
                    return badRequestWithErrorJCol("Invalid request", "406", "You have to specify a valid owner.", jcol);
                }
            } else {
                return badRequest(JsonCollectionUtil.addTemplate(Post.class, jcol).toString());
            }
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, CustomContentType.JSON_COLLECTION.getAcceptHeaderMessage());
        }
    }

    /**
     * Fetches the requested post(s) and returns Collection+JSON representation of the post(s).
     * @param id the id of the requested post, -1 for all posts visible for the current user (determined by access token)
     * @return ok with Collection+Json of requested post(s), bad request with received collection with errors or not acceptable
     * if the client does not accept Collection+JSON
     */
    public static Result get(final Long id) {
        if(request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            Collection collection = JsonCollectionUtil.getRequestedCollection(Post.class, id, "Post");
            Collection filteredCollection = new Collection.Builder().addItems(collection.filterItems(new net.hamnaberg.funclite.Predicate<Item>() {
                @Override
                public boolean apply(Item item) {
                    long postId = item.getData().getDataAsMap().get("id").getValue().get().asNumber().longValue();
                    Post post = Post.findById(postId);
                    long tokenUser = Long.parseLong(session().get("token_user"));
                    Account tokenUserAccount = Account.findById(tokenUser);
                    if (post.group != null) {
                        return (tokenUserAccount.groupMemberships.contains(Group.findById(post.group.id))
                                || (post.owner.id == tokenUser));
                    } else if (post.parent != null) {
                        return (tokenUserAccount.friends.contains(Account.findById(post.parent.id))
                                || (post.owner.id == tokenUser));
                    }
                    return (post.owner.id == tokenUser);
                }
            })).withHref(collection.getHref().get())
                    .withError(collection.getError().get())
                    .withTemplate(collection.getTemplate().get())
                    .addLinks(collection.getLinks())
                    .addQueries(collection.getQueries())
                    .build();

            return ok(filteredCollection.toString());
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, CustomContentType.JSON_COLLECTION.getAcceptHeaderMessage());
        }
    }

    /**
     * Constructs bad request response with received Collection+JSON with added error.
     * @param error the error
     * @param errorCode error code
     * @param description error description
     * @param jcol the received collection
     * @return bad request response with received Collection+JSON with added error
     */
    private static Result badRequestWithErrorJCol(String error, String errorCode, String description, Collection jcol) {
        Collection.Builder builder = new Collection
                .Builder()
                .addItems(jcol.getItems())
                .addLinks(jcol.getLinks())
                .addQueries(jcol.getQueries())
                .withError(Error.create(error, errorCode, description));
        if (jcol.hasTemplate()) {
            builder.withTemplate(jcol.getTemplate().get());
        }
        if (jcol.getHref().isSome()) {
            builder.withHref(jcol.getHref().get());
        }
        return badRequest(builder.build().toString());
    }

}
