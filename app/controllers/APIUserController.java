package controllers;

import models.Account;
import models.base.BaseModel;
import models.enums.CustomContentType;
import net.hamnaberg.json.*;
import net.hamnaberg.json.Error;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;


public class APIUserController extends BaseController {

    public static Result get(final long id) {
        if (request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            Error error = Error.EMPTY;
            List<Item> items;

            if (id == 0) {  // http GET .../users
//                items = BaseModel.getRequestedItems(Account.class, Account.all()); // Get all users from DB and limit them
                items = BaseModel.getRequestedItems(Account.class, Account::some);   // Better get only limited users from DB
            } else {  // http GET .../users/:id
                Account account = Account.findById(id);
                items = new ArrayList<>();
                if (account == null) {
                    error = net.hamnaberg.json.Error.create("Account not found", "404", "The " +
                                                            "requested account does not seem to exist.");
                } else {
                    items.add(Item.create(getBaseUri(), account.getProperties()));
                }
            }

            Collection collection = Collection.create(
                    getFullUri(),
                    new ArrayList<>(),
                    items,
                    new ArrayList<>(),
                    Template.create(Account.EXAMPLE.getProperties()),
                    error
            );

            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            return ok(collection.toString());
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, "Only accepting Accept header: " + CustomContentType.JSON_COLLECTION.getIdentifier());
        }
    }
}
