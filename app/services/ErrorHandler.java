package services;

import managers.AccountManager;
import managers.GroupManager;
import managers.PostManager;
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
    GroupManager groupManager;

    @Inject
    PostManager postManager;

    @Inject
    AccountManager accountManager;

    Configuration configuration;

    @Inject
    public ErrorHandler(Configuration configuration, Environment environment, OptionalSourceMapper optionalSourceMapper, Provider<Router> provider) {
        super(configuration, environment, optionalSourceMapper, provider);
        this.configuration = configuration;
    }

    protected F.Promise<Result> onProdServerError(Http.RequestHeader request, UsefulException exception) {
        Group group = groupManager.findByTitle(configuration.getString("htwplus.admin.group"));
        if(group != null){
            Post post = new Post();
            post.content = "Request: "+request+"\nError: "+exception;
            post.owner = accountManager.findByEmail(configuration.getString("htwplus.admin.mail"));
            post.group = group;
            postManager.create(post);
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
