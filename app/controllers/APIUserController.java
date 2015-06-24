package controllers;

import models.Account;
import models.enums.CustomContentType;
import net.hamnaberg.json.*;
import play.mvc.Result;

import java.net.URI;
import java.util.ArrayList;

/**
 * Created by richard on 17.06.15.
 */
public class APIUserController extends BaseController{

    public static Result view(final Long id) {
        Account account = Account.findById(id);

        if (request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            Collection collection;
            if (account == null) {
                collection = Collection.create(
                        URI.create(request().host() + request().path()),
                        new ArrayList<Link>(),
                        new ArrayList<Item>(),
                        new ArrayList<Query>(),
                        account.getTemplate(),
                        net.hamnaberg.json.Error.create("Account not found", "404", "The " +
                                "requested account does not seem to exist.")
                );
            } else {
                URI uri = URI.create(request().host() + request().path());
                ArrayList<Item> items = new ArrayList<Item>();
                items.add(Item.create(uri, account.getProperies()));
                collection = Collection.create(
                        uri,
                        new ArrayList<Link>(),
                        items,
                        new ArrayList<Query>(),
                        account.getTemplate(),
                        net.hamnaberg.json.Error.create("none", "none", "none")
                );
            }
            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            return ok(collection.toString());
        }
        return ok();
    }
    
}
