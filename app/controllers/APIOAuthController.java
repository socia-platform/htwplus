package controllers;

import models.Client;
import play.api.data.Form;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.net.URI;
import java.net.URISyntaxException;
import

/**
 * Created by richard on 01.07.15.
 */
public class APIOAuthController extends BaseController {

    public static Result addClient(String clientId, String clientSecret, String callback) {
        Client client = new Client();
        client.clientId = clientId;
        client.clientSecret = clientSecret;
        try {
            client.callBack = new URI(callback);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        client.create();
        return ok();
    }

    public static Result authorize(String clientId) {
        if (request().method().equals("GET")) {
            return ok("getmethod" + clientId); //redirect to authorization view
        } else {

            return ok("postmethod" + clientId);
        }
    }
}
