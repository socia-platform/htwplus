package services;

import managers.AccountManager;
import managers.GroupManager;
import managers.PostManager;
import models.Group;
import models.Post;
import models.services.NotificationService;
import org.apache.http.protocol.HTTP;
import play.Configuration;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.db.jpa.JPAApi;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import views.html.defaultpages.badRequest;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Iven on 08.12.2015.
 */
public class ErrorHandler extends DefaultHttpErrorHandler {

    private int MAX_FILESIZE;

    GroupManager groupManager;
    PostManager postManager;
    AccountManager accountManager;
    NotificationService notificationService;
    Configuration configuration;
    JPAApi jpaApi;

    @Inject
    public ErrorHandler(JPAApi jpaApi, Configuration configuration, Environment environment, OptionalSourceMapper optionalSourceMapper, Provider<Router> provider, AccountManager accountManager, GroupManager groupManager,
            PostManager postManager, NotificationService notificationService) {
        super(configuration, environment, optionalSourceMapper, provider);
        this.configuration = configuration;
        this.jpaApi = jpaApi;
        this.accountManager = accountManager;
        this.groupManager = groupManager;
        this.postManager = postManager;
        this.notificationService = notificationService;

        this.MAX_FILESIZE = configuration.getInt("media.maxSize.file");
    }

    protected CompletionStage<Result> onProdServerError(Http.RequestHeader request, UsefulException exception) {
        jpaApi.withTransaction(() -> {
            Group group = groupManager.findByTitle(configuration.getString("htwplus.admin.group"));
            if (group != null) {
                Post post = new Post();
                post.content = "Request: " + request + "\nError: " + exception;
                post.owner = accountManager.findByEmail(configuration.getString("htwplus.admin.mail"));
                post.group = group;
                postManager.createWithoutIndex(post);
            }
        });

        return CompletableFuture.completedFuture(Results.redirect(controllers.routes.Application.error()));
    }

    protected CompletionStage<Result> onForbidden(Http.RequestHeader request, String message) {
        jpaApi.withTransaction(() -> {
            Group group = groupManager.findByTitle(configuration.getString("htwplus.admin.group"));
            if (group != null) {
                Post post = new Post();
                post.content = "Request: " + request + "\nError: 403 - Forbidden (" + message + ")";
                post.owner = accountManager.findByEmail(configuration.getString("htwplus.admin.mail"));
                post.group = group;
                postManager.createWithoutIndex(post);
            }
        });

        return CompletableFuture.completedFuture(Results.redirect(controllers.routes.Application.index()));
    }

    protected CompletionStage<Result> onBadRequest(Http.RequestHeader request, String message) {
        jpaApi.withTransaction(() -> {
            Group group = groupManager.findByTitle(configuration.getString("htwplus.admin.group"));
            if (group != null) {
                Post post = new Post();
                post.content = "Request: " + request + "\nError: 400 - Bad Request (" + message + ")";
                post.owner = accountManager.findByEmail(configuration.getString("htwplus.admin.mail"));
                post.group = group;
                postManager.createWithoutIndex(post);
            }
        });

        return CompletableFuture.completedFuture(Results.redirect(controllers.routes.Application.index()));
    }

    protected CompletionStage<Result> onOtherClientError(Http.RequestHeader request, int errorCode, String message) {
        jpaApi.withTransaction(() -> {
            Group group = groupManager.findByTitle(configuration.getString("htwplus.admin.group"));
            if (group != null) {
                Post post = new Post();
                post.content = "Request: " + request + "\nError: " + errorCode + " - " + message;
                post.owner = accountManager.findByEmail(configuration.getString("htwplus.admin.mail"));
                post.group = group;
                postManager.createWithoutIndex(post);
            }
        });

        if (errorCode == Http.Status.REQUEST_ENTITY_TOO_LARGE) {
            return CompletableFuture.completedFuture(Results.status(errorCode, "Es sind maximal "+ MAX_FILESIZE + " MByte pro Datei möglich."));
        }

        return CompletableFuture.completedFuture(Results.status(errorCode, badRequest.render(request.method(), request.uri(), message)));
    }
}
