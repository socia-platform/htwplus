package controllers;

import play.Play;
import play.data.DynamicForm;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import views.html.landingpage;

import java.util.Date;

/**
 * Created by richard on 23.08.15.
 */
public class SecuredAuthorization extends Security.Authenticator {

    @Override
    public String getUsername(Http.Context ctx) {
        Secured sec = new Secured();
        return sec.getUsername(ctx);
    }

    @Override
    public Result onUnauthorized(Http.Context ctx) {
        ctx.session().put("originURL", ctx.request().uri());

        return ok(landingpage.render());
    }

}
