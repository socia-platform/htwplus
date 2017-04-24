package controllers;

import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AdminAction extends Action.Simple {

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {
        if (!Secured.isAdmin()) {
            return CompletableFuture.completedFuture(Results.redirect(controllers.routes.Application.index()));
        }
        Navigation.set(Navigation.Level.ADMIN);
        return delegate.call(ctx);
    }


}