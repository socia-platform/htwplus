package controllers;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import models.services.WebSocketService;
import play.Logger;
import play.db.jpa.Transactional;
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
        final Long accountId = WebSocketController.getCurrentAccountId();

        // called when the WebSocket Handshake is done.
        return new WebSocket<JsonNode>() {
            @Override
            public void onReady(In<JsonNode> in, Out<JsonNode> out) {
                if (accountId == null) {
                    out.close();
                    return;
                }

                @SuppressWarnings("unused")
                final ActorRef actor = WebSocketService.getInstance().getActorForAccountId(accountId, in, out);

                // For each event received on the socket,
                in.onMessage(wsMessage -> WebSocketService.getInstance().handleWsMessage(accountId, wsMessage));

                // When the socket is closed.
                in.onClose(() -> {
                    WebSocketService.getInstance().stopActor(accountId);
                    Logger.info("[WS] Disconnected User ID: " + accountId);
                });
            }
        };
    }

    /**
     * Returns the current account ID from current HTTP context.
     *
     * @return Account ID of current user
     */
    private static Long getCurrentAccountId() {
        try {
            return Long.valueOf(Http.Context.current().session().get("id"));
        } catch (Exception ex) {
            return null;
        }
    }
}
