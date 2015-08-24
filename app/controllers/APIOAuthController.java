package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.*;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Security;
import play.mvc.Result;
import views.html.OAuth2.authorizeClient;
import views.html.OAuth2.editClients;

import javax.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;


/**
 * Created by richard on 01.07.15.
 */
public class APIOAuthController extends BaseController {

    private static Form<Client> clientForm = Form.form(Client.class);

    @Security.Authenticated(Secured.class)
    @Transactional
    public static Result addClient() {
        if (request().method().equals("GET")) {
            if (Secured.isAdmin()) {
                return ok(editClients.render(Client.findAll(), clientForm));
            }
            else
                return unauthorized();
        } else {
            Form<Client> filledForm= clientForm.bindFromRequest();
            if (filledForm.data().get("clientName").isEmpty())
                filledForm.reject("Bitte gib einen Namen an.");
            if (filledForm.data().get("callback").isEmpty())
                filledForm.reject("Bitte gib eine Callback-URI an.");
            if(filledForm.hasErrors()) {
                return badRequest(editClients.render(Client.findAll(), filledForm));
            }
            Client client = new Client();
            client.clientName = filledForm.data().get("clientName");
            client.clientId = UUID.randomUUID().toString();
            client.clientSecret = UUID.randomUUID().toString();
            try {
                client.callback = new URI(filledForm.data().get("callback"));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            client.create();
            return ok(editClients.render(Client.findAll(), filledForm));
        }
    }

    @Security.Authenticated(SecuredAuthorization.class)
    public static Result authorize() {
        DynamicForm requestData = Form.form().bindFromRequest();
        for (String e : requestData.data().keySet())
            session().put(e, requestData.data().get(e));
        Client client = Client.findByClientId(requestData.get("client_id"));
        if (client != null) {
            return ok(authorizeClient.render(client.clientName, client.clientId));
        }
        else return badRequest();
    }

    @Security.Authenticated(SecuredAuthorization.class)
    @Transactional
    public static Result generateCode() {
        DynamicForm requestData = Form.form().bindFromRequest();
        if (session().get("response_type") != null && requestData.get("accepted").equals("true")) {
            if (session().get("response_type").equals("code")) {
                AuthorizationGrant grant = new AuthorizationGrant();
                grant.user = Component.currentAccount();
                grant.client = Client.findByClientId(session().get("client_id"));
                String authorizationCode = UUID.randomUUID().toString();
                grant.code = authorizationCode;
                grant.create();
                String red;
                if (session().get("redirect_uri") != null)
                    red = session().get("redirect_uri");
                else
                    red = (request().secure() ? "https://" : "http://") + grant.client.callback;
                red += "?code=" + authorizationCode;
                if (session().get("state") != null)
                    red += "&state=" + session().get("state");
                return redirect(red);
            }
        } else {
            return badRequest("You have to specify a response type.");
        }
        return internalServerError();
    }

    public static Result getToken() {
        DynamicForm requestData = Form.form().bindFromRequest();
        AuthorizationGrant grant = AuthorizationGrant.findByCode(requestData.get("code"));
        if (grant != null) {
            Client client = grant.client;
            Token token = new Token(client, Component.currentAccount(), null);
            token.user = grant.user;
            token.create();
            String res = "access_token=" + token.accessToken + "&token_type=bearer" + "&refresh_token="
                    + token.refreshToken + "&expires_in=-1";
            response().setContentType("application/x-www-form-urlencoded; charset=utf-8");
            return ok(res);
        } else
            return badRequest("Incorrect authorization code.");
    }

}
