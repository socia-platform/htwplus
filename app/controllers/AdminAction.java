package controllers;

import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class AdminAction extends Action.Simple {

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {
        if (!Secured.isAdmin()) {
            return delegate.call(ctx);
        }
        Navigation.set(Navigation.Level.ADMIN);
        return delegate.call(ctx);
    }


}