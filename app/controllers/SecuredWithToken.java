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
 * Created by richard on 02.07.15.
 */
public class SecuredWithToken extends Security.Authenticator {

    @Override
    public String getUsername(Http.Context ctx) {
        DynamicForm form = Form.form().bindFromRequest();
        Token token = Token.findByAccesToken(form.get("accessToken"));
        if (token != null && !token.hasExpired())
            return token.user.name;
        else
            return null;
    }

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
            return (unauthorized(collection.asJson()));
        } else {
            ctx.session().put("originURL", ctx.request().path());
            return unauthorized(landingpage.render());
        }
    }
}
