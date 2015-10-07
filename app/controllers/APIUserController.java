package controllers;

import models.Account;
import models.Friendship;
import models.enums.CustomContentType;
import net.hamnaberg.funclite.Predicate;
import net.hamnaberg.json.Collection;
import net.hamnaberg.json.Item;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import util.JsonCollectionUtil;

import java.util.LinkedList;
import java.util.List;

@Transactional
@Security.Authenticated(SecuredWithToken.class)
public class APIUserController extends BaseController {

    /**
     * Fetches the requested user(s) and returns Collection+JSON representation of the user(s).
     * @param id the id of the requested use, -1 for all uses visible for the current user (determined by access token and friendship status)
     * @return ok with Collection+Json of requested user(s), bad request with received collection with errors or not acceptable
     * if the client does not accept Collection+JSON
     */
    public static Result get(final long id) {
        if (request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            Collection collection = JsonCollectionUtil.getRequestedCollection(Account.class, id, "Account");
            Collection.Builder filterdCollectionBuilder = new Collection.Builder().addItems(collection.filterItems(new Predicate<Item>() {
                @Override
                public boolean apply(Item item) {
                    long tokenUser = Long.parseLong(session().get("token_user"));
                    Account tokenUserAccount = Account.findById(tokenUser);
                    boolean isFriend = false;
                    List<Long> friendsOfTokenUser = new LinkedList<Long>();
                    for (Friendship f : tokenUserAccount.friends) {
                        friendsOfTokenUser.add(f.friend.id);
                    }
                    long candidate = item.getData().getDataAsMap().get("id").getValue().get().asNumber().longValue();
                    if (friendsOfTokenUser.contains(candidate)) {
                        for (Friendship cf : Account.findById(candidate).friends) {
                            if (cf.friend.id == tokenUser) {
                                isFriend = true;
                            }
                        }
                    }
                    return isFriend;
                }
            })).withHref(collection.getHref().get())
                    .withError(collection.getError().get())
                    .withTemplate(collection.getTemplate().get())
                    .addLinks(collection.getLinks())
                    .addQueries(collection.getQueries());
            if (id == -1 || id == Long.parseLong(session().get("token_user"))) {
                filterdCollectionBuilder.addItem(JsonCollectionUtil
                        .getRequestedCollection(Account.class, Long.parseLong(session().get("token_user")), "Account")
                        .getFirstItem().get());
            }
            Collection filteredCollection = filterdCollectionBuilder.build();
            return ok(filteredCollection.toString());
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, CustomContentType.JSON_COLLECTION.getAcceptHeaderMessage());
        }
    }
}
