package controllers;

import play.Logger;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.SimpleResult;
import play.mvc.Http.Context;
import play.mvc.Result;

public class AdminAction extends Action.Simple {

	@Override
	public Promise<SimpleResult> call(Context ctx) throws Throwable {
		if(!Secured.isAdmin()){
			return Promise.pure(redirect(routes.Application.index()));
		}
		Navigation.set(Navigation.Level.ADMIN);
		return delegate.call(ctx);
	}


}