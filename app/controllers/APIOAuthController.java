package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import javafx.util.Pair;
import models.*;
import models.enums.AccountRole;
import models.enums.CustomContentType;
import models.services.LdapService;
import net.hamnaberg.json.Collection;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Security;
import play.mvc.Result;
import util.JsonCollectionUtil;
import views.html.OAuth2.authorizeClient;
import views.html.OAuth2.authorizeClientLogin;
import views.html.OAuth2.editClients;
import views.html.landingpage;

import javax.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static play.data.Form.form;

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


    public static Result authenticate() {
        DynamicForm form = form().bindFromRequest();
        String username = form.field("email").value();

        // save originURL before clearing the session (it gets cleared in defaultAuthenticate() and LdapAuthenticate())
        String redirect = session().get("/api/oauth2/allow");

        if (username.contains("@")) {
            return defaultAuthenticate(redirect);
        } else if (username.length() == 0) {
            flash("error", "Also deine Matrikelnummer brauchen wir schon!");
            return badRequest(landingpage.render());
        } else {
            return LdapAuthenticate(redirect);
        }
    }

    private static Result LdapAuthenticate(final String redirect) {
        Form<Login> form = form(Login.class).bindFromRequest();
        String matriculationNumber = form.field("email").value();
        String password = form.field("password").value();
        String rememberMe = form.field("rememberMe").value();

        // Clean the username
        matriculationNumber = matriculationNumber.trim().toLowerCase();

        // establish LDAP connection and try to get user data
        LdapService ldap = LdapService.getInstance();
        try {
            ldap.connect(matriculationNumber, password);
        } catch (LdapService.LdapConnectorException e) {
            flash("error", e.getMessage());
            Component.addToContext(Component.ContextIdent.loginForm, form);
            return badRequest(landingpage.render());
        }

        // try to find user in DB, set role if found (default STUDENT role)
        Account account = Account.findByLoginName(matriculationNumber);
        AccountRole role = AccountRole.STUDENT;
        if (ldap.getRole() != null) {
            role = ldap.getRole();
        }

        // if user is not found in DB, create new user from LDAP data, otherwise update user data
        if (account == null) {
            account = new Account();
            Logger.info("New Account for " + matriculationNumber + " will be created.");
            account.firstname = ldap.getFirstName();
            account.lastname = ldap.getLastName();
            account.loginname = matriculationNumber;
            account.password = "LDAP - not needed";
            Random generator = new Random();
            account.avatar = String.valueOf(generator.nextInt(9));
            account.role = role;
            account.create();
        } else {
            account.firstname = ldap.getFirstName();
            account.lastname = ldap.getLastName();
            account.role = role;
            account.update();
        }

        // re-create session, set user
        session().clear();
        session("id", account.id.toString());
        if (rememberMe != null) {
            session("rememberMe", "1");
        }

        return redirect(redirect);
    }

    private static Result defaultAuthenticate(final String redirect) {
        Form<Login> loginForm = form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            flash("error", loginForm.globalError().message());
            Component.addToContext(Component.ContextIdent.loginForm, loginForm);
            return badRequest(landingpage.render());
        } else {
            session().clear();
            session("email", loginForm.get().email);
            session("id", Account.findByEmail(loginForm.get().email).id.toString());
            session("firstname", Account.findByEmail(loginForm.get().email).firstname);
            if (loginForm.get().rememberMe != null) {
                session("rememberMe", "1");
            }

            return redirect(redirect);
        }
    }

    @Security.Authenticated(SecuredAuthorization.class)
    public static Result authorize() {
        DynamicForm requestData = Form.form().bindFromRequest();
        for (String e : requestData.data().keySet())
            session().put(e, requestData.data().get(e));
        Http.Session s = session();
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

    /*@Security.Authenticated(Secured.class)
    @Transactional*/
    public static Result getToken() {
        DynamicForm requestData = Form.form().bindFromRequest();
        AuthorizationGrant grant = AuthorizationGrant.findByCode(requestData.get("code"));
        if (grant != null) {
            Client client = grant.client;
            Token token = new Token(client, Component.currentAccount(), null);
            token.user = grant.user;
            token.create();
            ObjectNode result = Json.newObject();
            result.put("access_token", token.accessToken);
            result.put("token_type", "bearer");
            result.put("expires_in", -1);
            result.put("refresh_token", token.refreshToken);
            /*Collection collection = JsonCollectionUtil.getRequestedCollection(Token.class, Lists.newArrayList(token));
            response().setContentType(CustomContentType.JSON_COLLECTION.getIdentifier());*/
            /*return ok(result);*/
            String res = "access_token=" + token.accessToken + "&token_type=bearer";
            response().setContentType("application/x-www-form-urlencoded; charset=utf-8");
            return ok(res);
        } else
            return badRequest("Incorrect authorization code.");
    }

}
