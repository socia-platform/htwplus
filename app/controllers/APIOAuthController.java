package controllers;

import models.Client;
import models.Grant;
import models.Token;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Security;
import play.mvc.Result;
import views.html.OAuth2.authorizeClient;
import views.html.OAuth2.editClients;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * Created by richard on 01.07.15.
 */
public class APIOAuthController extends BaseController {

    private static Form<Client> clientForm = Form.form(Client.class);

    @Security.Authenticated(Secured.class)
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

    @Security.Authenticated(Secured.class)
    public static Result authorize() {
        DynamicForm requestData = Form.form().bindFromRequest();
        if (request().method().equals("GET")) {
            /*Client client = Client.findByClientId(requestData.get("clientId"));
            if (client != null) {
                return ok(authorizeClient.render(client.clientName));
            }*/
            return ok(authorizeClient.render("App"));
        } else if (requestData.get("accepted").equals("true")) {
            Grant grant = new Grant();
            grant.user = Component.currentAccount();
            grant.client = Client.findByClientId(requestData.get("clientId"));
            String authorizationCode = UUID.randomUUID().toString();
            grant.code = authorizationCode;
            grant.create();
            return redirect(grant.client.callback + "?code=" + authorizationCode);
        }
        return internalServerError();
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
