package controllers;

import models.Client;
import models.Grant;
import models.Token;
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

    @Security.Authenticated(Secured.class)
    public static Result getToken(String clientId, String clientSecret, String code, String callback) {
        if (Grant.findByUserId(Component.currentAccount().id).code.equals(code)) {
            Token token = new Token();
            token.accessToken = UUID.randomUUID().toString();
            token.client = Client.findByClientId(clientId);
            token.user = Component.currentAccount();
            token.create();
            return redirect(callback + "?access_token=" + token.accessToken);
        } else
            return badRequest("Incorrect authorization code.");
    }

}
