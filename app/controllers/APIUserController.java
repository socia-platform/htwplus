package controllers;

import com.google.common.collect.Lists;
import models.Account;
import models.enums.CustomContentType;
import net.hamnaberg.json.*;
import play.mvc.Result;
import util.JsonCollectionUtil;


public class APIUserController extends BaseController {
    public static Result get(final long id) {
        if (request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            Collection collection;
            if (id >= 0) { // http GET .../users/:id
                Account account = Account.findById(id);

                if (account == null) {
                    collection = JsonCollectionUtil.getErrorCollection(Account.class,
                            "Account not found", "404", "The requested account does not seem to exist.");
                } else {
                    collection = JsonCollectionUtil.getRequestedCollection(Account.class, Lists.newArrayList(account));
                }
            } else { // http GET ...users
//                collection = JsonCollectionUtil.getRequestedCollection(Account.class, Account.all(), Error.EMPTY); // Get all users from DB and limit them
                collection = JsonCollectionUtil.getRequestedCollection(Account.class, Account::some);   // Better get only limited users from DB
            }

            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            return ok(collection.toString());
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, "Only accepting Accept header: " + CustomContentType.JSON_COLLECTION.getIdentifier());
        }
    }
}
