package controllers;

import models.Client;
import models.Grant;
import play.mvc.Security;
import play.mvc.Result;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

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

    @Security.Authenticated(Secured.class)
    public static Result authorize(String clientId, String user) {
        if (request().method().equals("GET")) {
            return ok("getmethod" + clientId); //redirect to authorization view
        } else {
            Grant grant = new Grant();
            grant.user = Component.currentAccount();
            grant.client = Client.findByClientId(clientId);
            String authorizationCode = UUID.randomUUID().toString();
            grant.code = authorizationCode;
            grant.create();
            return redirect(grant.client.callBack + "?code=" + authorizationCode);
        }
    }
}
