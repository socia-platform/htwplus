package controllers;

import models.Client;
import models.Grant;
import models.Token;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Security;
import play.mvc.Result;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * Created by richard on 01.07.15.
 */
public class APIOAuthController extends BaseController {

    public static Result addClient() {
        DynamicForm requestData = Form.form().bindFromRequest();
        Client client = new Client();
        client.clientName = requestData.get("clientName");
        client.clientId = UUID.randomUUID().toString();
        client.clientSecret = UUID.randomUUID().toString();
        try {
            client.callback = new URI(requestData.get("callbackUri"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        client.create();
        return ok();
    }

    @Security.Authenticated(Secured.class)
    public static Result authorize() {
        if (request().method().equals("GET")) {
            return ok("getmethod"); //redirect to authorization view
        } else {
            DynamicForm requestData = Form.form().bindFromRequest();
            Grant grant = new Grant();
            grant.user = Component.currentAccount();
            grant.client = Client.findByClientId(requestData.get("clientId"));
            String authorizationCode = UUID.randomUUID().toString();
            grant.code = authorizationCode;
            grant.create();
            return redirect(grant.client.callback + "?code=" + authorizationCode);
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result getToken() {
        DynamicForm requestData = Form.form().bindFromRequest();
        Grant grant = Grant.findByUserId(Component.currentAccount().id);
        if ((grant != null) && (grant.code.equals(requestData.get("code")))) {
            Client client = grant.client;
            Token token = new Token();
            token.accessToken = UUID.randomUUID().toString();
            token.client = client;
            token.user = Component.currentAccount();
            token.create();
            return redirect(client.callback.toString() + "?access_token=" + token.accessToken);
        } else
            return badRequest("Incorrect authorization code.");
    }

}
