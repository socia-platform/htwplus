package services;

import models.Account;
import models.Group;
import models.Post;
import play.Configuration;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by Iven on 08.12.2015.
 */
public class ErrorHandler extends DefaultHttpErrorHandler {

    @Inject
    Group group;

    @Inject
    Post post;

    Configuration configuration;

    @Inject
    public ErrorHandler(Configuration configuration, Environment environment, OptionalSourceMapper optionalSourceMapper, Provider<Router> provider) {
        super(configuration, environment, optionalSourceMapper, provider);
        this.configuration = configuration;
    }

    protected F.Promise<Result> onProdServerError(Http.RequestHeader request, UsefulException exception) {
        group = group.findByTitle(configuration.getString("htwplus.admin.group"));
        if(group != null){
            post.content = "Request: "+request+"\nError: "+exception;
            post.owner = Account.findByEmail(configuration.getString("htwplus.admin.mail"));
            post.group = group;
            post.create();
        }

        return F.Promise.<Result>pure(
                Results.redirect(controllers.routes.Application.error())
        );
    }

    protected F.Promise<Result> onForbidden(Http.RequestHeader request, String message) {
        return F.Promise.<Result>pure(
                Results.forbidden("You're not allowed to access this resource.")
        );
    }
}
