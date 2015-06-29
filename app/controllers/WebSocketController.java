package controllers;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import models.Account;
import models.services.WebSocketService;
import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Security;
import play.mvc.WebSocket;


@Transactional
@Security.Authenticated(Secured.class)
public class WebSocketController extends BaseController {
    /**
     * Handles the web socket channel by invoking Akka actor.
     *
     * @return Web socket instance including JSON nodes
     */
    @Transactional(readOnly = true)
    public static WebSocket<JsonNode> webSocket() {
        final Account account = WebSocketController.getCurrentAccount();

        // called when the WebSocket Handshake is done.
        return new WebSocket<JsonNode>() {
            @Override
            public void onReady(In<JsonNode> in, Out<JsonNode> out) {
                if (account == null) {
                    out.close();
                    return;
                }

                @SuppressWarnings("unused")
                final ActorRef actor = WebSocketService.getInstance().getActorForAccount(account, in, out);

                // For each event received on the socket,
                in.onMessage(new F.Callback<JsonNode>() {
                    public void invoke(JsonNode wsMessage) {
                        WebSocketService.getInstance().handleWsMessage(account, wsMessage);
                    }
                });

                // When the socket is closed.
                in.onClose(new F.Callback0() {
                    public void invoke() {
                        WebSocketService.getInstance().stopActor(account);
                        //Logger.info("[WS] Disconnected User ID: " + account.id);
                    }
                });
            }
        };
    }

    /**
     * Returns the current account ID from current HTTP context.
     *
     * @return Account of current user
     */
    private static Account getCurrentAccount() {
        try {
            return JPA.withTransaction(new F.Function0<Account>() {
                @Override
                public Account apply() throws Throwable {
                    return Account.findById(Long.valueOf(Http.Context.current().session().get("id")));
                }
            });
        } catch (Throwable throwable) {
            Logger.error("Error while fetching account for WebSocket initialization: " + throwable.getMessage());
            return null;
        }
    }
}
