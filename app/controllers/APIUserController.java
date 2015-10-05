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

@Transactional
@Security.Authenticated(SecuredWithToken.class)
public class APIUserController extends BaseController {
    public static Result get(final long id) {
        if (request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            Collection collection = JsonCollectionUtil.getRequestedCollection(Account.class, id, "Account");
            Collection filterdCollection = new Collection.Builder().addItems(collection.filterItems(new Predicate<Item>() {
                @Override
                public boolean apply(Item item) {
                    long tokenUser = Long.parseLong(session().get("token_user"));
                    Account tokenUserAccount = Account.findById(tokenUser);
                    boolean isFriend = false;
                    for (Friendship f : tokenUserAccount.friends) {
                        if (f.friend.id == item.getData().getDataAsMap().get("id").getValue().get().asNumber().longValue()) {
                            isFriend = true;
                        }
                    }
                    return isFriend;
                }
            })).withHref(collection.getHref().get())
                    .withError(collection.getError().get())
                    .withTemplate(collection.getTemplate().get())
                    .addLinks(collection.getLinks())
                    .addQueries(collection.getQueries())
                    .build();
            return ok(filterdCollection.toString());
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, CustomContentType.JSON_COLLECTION.getAcceptHeaderMessage());
        }
    }
}
