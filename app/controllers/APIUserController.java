package controllers;

import models.Account;
import models.enums.CustomContentType;
import net.hamnaberg.json.*;
import net.hamnaberg.json.Error;
import play.mvc.Result;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class APIUserController extends BaseController {

    public static Result get(final Long id) {
        if (request().getHeader("Accept").contains(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            URI uri = URI.create(request().host() + request().path());
            Error error = Error.EMPTY;
            List<Item> items = null;

            if (id == 0) {  // http GET .../users
                items = Account.all()
                        .stream()
                        .map(a -> Item.create(URI.create(uri.toString() + "/" + a.id), a.getProperies()))
                        .collect(Collectors.toList());

            } else {  // http GET .../users/:id
                Account account = Account.findById(id);
                items = new ArrayList<Item>();
                if (account == null) {
                    error = net.hamnaberg.json.Error.create("Account not found", "404", "The " +
                                                            "requested account does not seem to exist.");
                } else {
                    items.add(Item.create(uri, account.getProperies()));
                }
            }

            Collection collection = Collection.create(
                    uri,
                    new ArrayList<Link>(),
                    items,
                    new ArrayList<Query>(),
                    Template.create(Account.EXAMPLE.getProperies()),
                    error
            );

            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            return ok(collection.toString());
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, "Only accepting Accept header: " + CustomContentType.JSON_COLLECTION.getIdentifier());
        }
    }
}
