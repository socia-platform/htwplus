package controllers;

import com.google.common.collect.Lists;
import models.Client;
import models.UserGrant;
import models.Token;
import models.enums.CustomContentType;
import net.hamnaberg.json.Collection;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Security;
import play.mvc.Result;
import util.JsonCollectionUtil;
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

    @Security.Authenticated(Secured.class)
    @Transactional
    public static Result authorize() {
        DynamicForm requestData = Form.form().bindFromRequest();
        if (request().method().equals("GET")) {
            Client client = Client.findByClientId(requestData.get("clientId"));
            if (client != null) {
                return ok(authorizeClient.render(client.clientName, client.clientId));
            }
        } else if (requestData.get("accepted").equals("true")) {
            UserGrant grant = new UserGrant();
            grant.user = Component.currentAccount();
            grant.client = Client.findByClientId(requestData.get("clientId"));
            String authorizationCode = UUID.randomUUID().toString();
            grant.code = authorizationCode;
            grant.create();
            String red = (request().secure() ? "https://" : "http://") + grant.client.callback + "?authorizationCode=" + authorizationCode;
            return redirect(red);
        }
        return internalServerError();
    }

    @Security.Authenticated(Secured.class)
    @Transactional
    public static Result getToken() {
        DynamicForm requestData = Form.form().bindFromRequest();
        UserGrant grant = UserGrant.findByCode(requestData.get("authorizationCode"));
        if (grant != null) {
            Client client = grant.client;
            Token token = new Token(client, Component.currentAccount(), null);
            token.create();
            Collection collection = JsonCollectionUtil.getRequestedCollection(Token.class, Lists.newArrayList(token));
            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());
            return ok(collection.asJson());
        } else
            return badRequest("Incorrect authorization code.");
    }

}
