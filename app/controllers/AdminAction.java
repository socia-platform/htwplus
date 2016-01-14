package controllers;

import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

public class AdminAction extends Action.Simple {

    @Override
    public Promise<Result> call(Context ctx) throws Throwable {
        if (!Secured.isAdmin()) {
            return Promise.pure(redirect(controllers.routes.Application.index()));
        }
        Navigation.set(Navigation.Level.ADMIN);
        return delegate.call(ctx);
    }


}