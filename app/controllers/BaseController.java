package controllers;

import play.db.jpa.Transactional;
import play.mvc.*;

import java.net.URI;

@Transactional
@With(Component.class)
public class BaseController extends Controller {

    public static Result statusWithWarning(int status, String warning) {
        response().setHeader("Warning", warning);
        return status(status);
    }

    public static URI getBaseUri() {
        Http.Request request = request();
        // I don't like the way they did it, but that's the way this framework fixed the protocol thing
        // https://github.com/playframework/playframework/issues/842
        String protocol = request.secure() ? "https://" : "http://";
        return URI.create(protocol + request.host());
    }

    public static URI getFullUri() {
        Http.Request request = request();
        // I don't like the way they did it, but that's the way this framework fixed the protocol thing
        // https://github.com/playframework/playframework/issues/842
        String protocol = request.secure() ? "https://" : "http://";
        return URI.create(protocol + request.host() + request.uri());
    }
}