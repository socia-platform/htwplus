package controllers;

import models.Token;
import models.enums.CustomContentType;
import net.hamnaberg.json.*;
import net.hamnaberg.json.Error;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import util.JsonCollectionUtil;
import views.html.landingpage;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


/**
 * A standard authenticator to secure action with access tokens.
 */
public class SecuredWithToken extends Security.Authenticator {

    /**
     * Tries to determine the user name of the current user by checking the access token.
     * @param ctx the context
     * @return the user name if it can be found, null otherwise
     */
    @Override
    public String getUsername(Http.Context ctx) {
        DynamicForm form = Form.form().bindFromRequest();
        Token token = Token.findByAccesToken(form.get("access_token"));
        if (token != null && !token.hasExpired()) {
            ctx.session().put("token_user", (token.user.id).toString());
            return token.user.name;
        }
        else
            return null;
    }

    /**
     * Creates a Collection+JSON with error, to inform the client that no valid access token was provided or redirects
     * to landing page.
     * @param ctx the context
     * @return Collection+JSON with error if client accepts Collection+JSON, redirects to landing page with unauthorized
     * status otherwise
     */
    @Override
    public Result onUnauthorized(Http.Context ctx) {
        // token outdated? save originURL to prevent redirect to index page after login
        if (ctx.request().accepts(CustomContentType.JSON_COLLECTION.getIdentifier())) {
            Error error = Error.create("Invalid token ", "401", "Please submit a valid AccessToken.");
            String protocol = ctx.request().secure() ? "https://" : "http://";
            URI uri = URI.create(protocol + ctx.request().host() + ctx.request().path());
            Collection collection = Collection.create(
                    uri,
                    new ArrayList<Link>(),
                    new ArrayList<Item>(),
                    new ArrayList<Query>(),
                    Template.create(),
                    error
            );
            ctx.response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            return (unauthorized(collection.toString()));
        } else {
            ctx.session().put("originURL", ctx.request().path());
            return unauthorized(landingpage.render());
        }
    }
}
