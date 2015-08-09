package controllers;

import models.Account;
import models.enums.CustomContentType;
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
            return ok(JsonCollectionUtil.getRequestedCollectionString(Account.class, id, "Account"));
        } else {
            return statusWithWarning(NOT_ACCEPTABLE, CustomContentType.JSON_COLLECTION.getAcceptHeaderMessage());
        }
    }
}
