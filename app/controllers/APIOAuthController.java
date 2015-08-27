package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.*;
import models.enums.OAuth2ErrorCodes;
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
import java.util.*;


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
        for (String s : requestData.data().keySet()) {
            session().put(s, requestData.data().get(s));
        }
        Map<String, String> errs = getAuthorizationRequestErrors(session());
        if (errs.size() == 0) {
            Client client = Client.findByClientId(session().get("client_id"));
            String redURI = session().get("redirect_uri") != null ? session().get("redirect_uri") : client.callback.toString();
            if (session().get("accepted").equals("true")) {
                AuthorizationGrant grant = new AuthorizationGrant(Component.currentAccount(), client, new Long(60));
                grant.create();
                response().setContentType("application/x-www-form-urlencoded; charset=utf-8");
                StringBuilder res = new StringBuilder()
                        .append(redURI)
                        .append("?code=")
                        .append(grant.code)
                        .append("&expires_in=")
                        .append(grant.expiresIn());
                return redirect(res.toString());
            } else {
                return errorResponse(OAuth2ErrorCodes.ACCESS_DENIED.getIdentifier(), "end-user denied authorization", redURI);
            }
        } else {
            String redURI = null;
            if (requestData.get("redirect_uri") != null) {
                redURI = requestData.get("redirect_uri");
            } else if (requestData.get("client_id") != null) {
                Client client = Client.findByClientId(requestData.get("client_id"));
                if (client != null) {
                    redURI = client.callback.toString();
                }
            }
            if (redURI != null) {
                response().setContentType("application/x-www-form-urlencoded; charset=utf-8");
                return errorResponse(OAuth2ErrorCodes.INVALID_REQUEST.getIdentifier(), "end-user denied authorization", redURI);
            } else {
                StringBuilder errorString = new StringBuilder();
                for (String s : errs.keySet()) {
                    errorString.append(s).append(": ").append(errs.get(s)).append("; ");
                }
                return badRequest(errorString.toString());
            }
        }
    }



    public static Result getToken() {
        DynamicForm requestData = Form.form().bindFromRequest();
        Map<String, String> errs = getTokenRequestErrors(requestData.data());
        if (errs.size() == 0) {
            if (requestData.get("grant_type").equals("authorization_code")) {
                AuthorizationGrant grant = AuthorizationGrant.findByCode(requestData.get("code"));
                Token token = new Token(grant.client, grant.user, new Long(3600));
                token.create();
                return tokenResponseJson(token);
            } else if (requestData.get("grant_type").equals("refresh_token")) {
                Token token = Token.findByRefreshToken(requestData.get("refresh_token"));
                token.refresh(new Long(3600));
                token.update();
                return tokenResponseJson(token);
            } else {
                return errorResponseJson("invalid_request", "only serving grant_type authorization_code and refresh_token");
            }
        } else {
            StringBuilder errDescriptions = new StringBuilder();
            for (String k : errs.keySet()) {
                errDescriptions.append(k).append(": ").append(errs.get(k)).append("; ");
            }
            return errorResponseJson("invalid_request", errDescriptions.toString());
        }
    }

    private static Map<String, String> getAuthorizationRequestErrors(Map<String, String> requestData) {
        HashMap<String, String> errs = new HashMap<>();
        String[] terms = {"accepted", "response_type", "client_id"};
        List<String> missing = util.RequestTools.getNull(Arrays.asList(terms), requestData);
        if (missing.size() != 0) {
            for (String s : missing) {
                errs.put(s, "missing");
            }
        } else if (requestData.get("response_type").equals("code")) {
            Client client = Client.findByClientId(requestData.get("client_id"));
            if (client == null) {
                errs.put("client_id", "invalid");
            }
        } else {
            errs.put("response_type", "only serving code");
        }
        return errs;
    }

    private static Map<String, String> getTokenRequestErrors(Map<String, String> requestData) {
        HashMap<String, String> errs = new HashMap<>();
        String[] terms = {"grant_type", "client_id", "client_secret"};
        List<String> missing = util.RequestTools.getNull(Arrays.asList(terms), requestData);
        if (missing.size() != 0) {
            for (String s : missing) {
                errs.put(s, "missing");
            }
        } else if (Client.findByClientId(requestData.get("client_id")) != null) {
            Client client = Client.findByClientId(requestData.get("client_id"));
            if (client.clientSecret.equals(requestData.get("client_secret"))) {
                if (requestData.get("grant_type").equals("authorization_code")) {
                    if (AuthorizationGrant.findByCode(requestData.get("code")) == null)
                        errs.put("code", "invalid");
                    } else if (AuthorizationGrant.findByCode(requestData.get("code")).client != client) {
                    errs.put("client_id", "provided client_id does not fit authorization code");
                    }
                } else if (requestData.get("grant_type").equals("refresh_token")) {
                    if (Token.findByRefreshToken(requestData.get("refresh_token")) == null) {
                        errs.put("refresh_token", "invalid");
                    } else if (Token.findByRefreshToken(requestData.get("refresh_token")).client != client) {
                        errs.put("client_id", "provided client_id does not fit refresh_token");
                } else {
                    errs.put("grant_type", "invalid, only serving code and refresh_token");
                }
            } else {
                errs.put("client_secret", "invalid");
            }
        }
        return errs;
    }

    private static Result errorResponse(String errorCode, String description, String redirect) {
        response().setContentType("application/x-www-form-urlencoded; charset=utf-8");
        StringBuilder call = new StringBuilder()
                .append(redirect)
                .append("?error=")
                .append(errorCode)
                .append("&error_description")
                .append(description);
        return redirect(call.toString());
    }

    private static Result errorResponseJson(String errorCode, String description) {
        ObjectNode json = Json.newObject()
                .put("error", errorCode)
                .put("error_description", description);
        return badRequest(json);
    }

    private static Result tokenResponseJson(Token token) {
        ObjectNode json = Json.newObject()
                .put("access_token", token.accessToken)
                .put("token_type", "bearer")
                .put("refresh_token", token.refreshToken)
                .put("expires_in", token.expiresIn());
        return ok(json);
    }

}
