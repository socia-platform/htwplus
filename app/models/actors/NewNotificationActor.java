package models.actors;

import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.NewNotification;
import play.libs.Json;
import play.mvc.WebSocket;

import java.util.List;

/**
 * New notification actor is used in NewNotificationController to ask for new
 * notifications asynchronously.
 */
public class NewNotificationActor extends UntypedActor {
    WebSocket.In<JsonNode> in;
    WebSocket.Out<JsonNode> out;
    Long accountId;

    /**
     * Constructor, sets the web socket attributes.
     *
     * @param in WebSocket input stream
     * @param out WebSocket output stream
     * @param accountId Current user ID of this context
     */
    public NewNotificationActor(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out, Long accountId) {
        this.in = in;
        this.out = out;
        this.accountId = accountId;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        try {
            if (message.equals("getNotifications")) {
                JsonNode jsonResult = Json.toJson(NewNotification.findRenderedContentByAccount(this.accountId));
                this.out.write(jsonResult);
            } else {
                unhandled(message);
                this.context().stop(this.self());
            }
        } catch (Throwable ex) {
            unhandled(ex.getMessage());
            this.context().stop(this.self());
        }
    }
}
