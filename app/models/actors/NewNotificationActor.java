package models.actors;

import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.NewNotification;
import play.libs.Json;
import play.mvc.WebSocket;
import java.util.ArrayList;
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
                // get notification list and build up JSON list containing JSON objects (with notification data)
                List<NewNotification> notifications = NewNotification.findByAccount(this.accountId);
                List<ObjectNode> jsonList = new ArrayList<ObjectNode>(notifications.size());

                for (NewNotification notification : notifications) {
                    ObjectNode jsonListElement = Json.newObject();
                    jsonListElement.put("is_read", notification.isRead);
                    jsonListElement.put("content", notification.rendered);
                    jsonListElement.put("id", notification.id);
                    jsonListElement.put("updated", notification.updatedAt.getTime());
                    jsonListElement.put("created", notification.createdAt.getTime());
                    jsonList.add(jsonListElement);
                }

                this.out.write(Json.toJson(jsonList));
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
