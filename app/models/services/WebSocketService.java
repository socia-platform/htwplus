package models.services;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Account;
import models.Friendship;
import models.actors.WebSocketActor;
import models.base.IJsonNodeSerializable;
import play.Logger;
import play.libs.Akka;
import play.libs.Json;
import play.mvc.WebSocket;

import java.lang.reflect.Method;
import java.util.*;

/**
 * WebSocket service that is handling all active WebSocket actors.
 */
public class WebSocketService {
    public static final String WS_METHOD_SEND_CHAT = "SendChat";
    public static final String WS_METHOD_RECEIVE_CHAT = "ReceiveChat";
    public static final String WS_METHOD_RECEIVE_NOTIFICATION = "ReceiveNotification";
    public static final String WS_RESPONSE_OK = "OK";
    public static final String WS_RESPONSE_ERROR = "ERROR";

    /**
     * Singleton instance
     */
    private static WebSocketService instance = null;

    /**
     * Holds all active WebSocket actors
     */
    Map<Long, ActorRef> accountActor = new HashMap<>();

    /**
     * Private constructor for singleton instance
     */
    private WebSocketService() { }

    /**
     * Returns the singleton instance.
     *
     * @return NotificationHandler instance
     */
    public static WebSocketService getInstance() {
        if (WebSocketService.instance == null) {
            WebSocketService.instance = new WebSocketService();
        }

        return WebSocketService.instance;
    }

    /**
     * Invokes a new ActorRef instance of type WebSocketActor for an account ID.
     *
     * @param accountId Account ID
     * @param in WebSocket input stream
     * @param out WebSocket output stream
     */
    public void invokeActor(Long accountId, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        if (this.getActorForAccountId(accountId) != null) {
            return;
        }


        this.accountActor.put(accountId, Akka.system().actorOf(Props.create(WebSocketActor.class, accountId, in, out)));
    }

    /**
     * Returns an ActorRef instance for an account ID if available, otherwise null.
     *
     * @param accountId Account ID
     * @return ActorRef instance if available for account ID, otherwise null
     */
    public ActorRef getActorForAccountId(Long accountId) {
        if (!this.accountActor.containsKey(accountId)) {
            return null;
        }

        return this.accountActor.get(accountId);
    }

    /**
     * Returns an ActorRef instance for an account ID. If no ActorRef available the invokeActor() method
     * is called and the new ActorRef instance is returned.
     *
     * @param accountId Account ID
     * @param in WebSocket input stream
     * @param out WebSocket output stream
     * @return ActorRef instance
     */
    public ActorRef getActorForAccountId(Long accountId, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        if (this.getActorForAccountId(accountId) == null) {
            this.invokeActor(accountId, in, out);
        }

        return this.getActorForAccountId(accountId);
    }

    /**
     * Stops an ActorRef for an account ID.
     *
     * @param accountId Account ID
     */
    public void stopActor(Long accountId) {
        if (this.getActorForAccountId(accountId) == null) {
            return;
        }

        ActorRef stoppingActorRef = this.accountActor.remove(accountId);
        Akka.system().stop(stoppingActorRef);
    }

    /**
     * Returns a List of ObjectNode instances by a list of IJsonNodeSerializable implementing instances.
     *
     * @param serializableList List of IJsonNodeSerializable implementing instances
     * @return List of ObjectNode instances
     */
    public List<ObjectNode> getJsonList(List<? extends IJsonNodeSerializable> serializableList) {
        List<ObjectNode> jsonList = new ArrayList<>(serializableList.size());
        for (IJsonNodeSerializable serializable : serializableList) {
            jsonList.add(serializable.getAsJson());
        }

        return jsonList;
    }

    /**
     * Handles a WebSocket message
     *
     * @param accountId Account ID
     * @param wsMessage WebSocket message as JsonNode object
     */
    public void handleWsMessage(Long accountId, JsonNode wsMessage) {
        // Log events to the console
        Logger.info("[WS] Received (User ID: " + accountId + "): " + wsMessage.toString());

        if (!wsMessage.has("method")) {
            Logger.error("[WS] No method received");
            return;
        }

        ActorRef senderActor = this.getActorForAccountId(accountId);
        senderActor.tell(wsMessage, null);
    }

    /**
     * Tries to invoke a WebSocket method.
     *
     * @param wsMessage WebSocket message as JsonNode object
     * @param senderActor Sending actor
     * @param sender Sending account
     * @return WebSocket response
     */
    public JsonNode invokeWsMethod(JsonNode wsMessage, ActorRef senderActor, Account sender) {
        String methodName = "ws" + wsMessage.get("method").asText();
        Class[] classParameters = new Class[] { JsonNode.class, ActorRef.class, Account.class };
        Object[] invocationParameters = new Object[] { wsMessage, senderActor, sender };

        try {
            // retrieve callable method and set accessible to true, as the methods are not declared
            // as public, finally invoke the method
            Method wsMethod = this.getClass().getDeclaredMethod(methodName, classParameters);
            wsMethod.setAccessible(true);

            return (JsonNode)wsMethod.invoke(this, invocationParameters);
        } catch (Exception e) {
            return this.errorResponse("Undefined error");
        }
    }

    /**
     * Returns a WebSocket error.
     *
     * @param errorMessage Error text message
     * @return WebSocket response
     */
    private JsonNode errorResponse(String errorMessage) {
        ObjectNode node = Json.newObject();
        node.put("code", WebSocketService.WS_RESPONSE_ERROR);
        node.put("text", errorMessage);

        return Json.toJson(node);
    }

    /**
     * Returns a success response template as ObjectNode instance with mandatory meta data.
     *
     * @param method WebSocket method as String
     * @return ObjectNode instance
     */
    public ObjectNode successResponseTemplate(String method) {
        ObjectNode node = Json.newObject();
        node.put("method", method);
        node.put("code", WebSocketService.WS_RESPONSE_OK);
        node.put("time", (new Date()).getTime());

        return node;
    }

    /**
     * WebSocket method when sending chat.
     *
     * @param wsMessage WebSocket message as JsonNode object
     * @param senderActor Sending actor
     * @param sender Sending account
     * @return WebSocket response
     */
    @SuppressWarnings("unused")
    private JsonNode wsSendChat(JsonNode wsMessage, ActorRef senderActor, Account sender) {
        // validate given parameters
        if (!wsMessage.has("recipient") || !wsMessage.has("text")) {
            return this.errorResponse("Could not send chat message, either no \"recipient\" or \"text\" information.");
        }

        Long recipientAccountId = wsMessage.get("recipient").asLong();
        String text = wsMessage.get("text").asText();
        ActorRef recipientActor = this.getActorForAccountId(recipientAccountId);
        Account recipient = Account.findByIdTransactional(recipientAccountId);

        // check, if the recipient is online
        if (recipientActor == null) {
            return this.errorResponse("Recipient not online");
        }

        // sender should not be recipient
        if (recipientActor.equals(senderActor)) {
            return this.errorResponse("Cannot send chat to yourself");
        }
        // check, if sender and recipient are friends
        if (sender == null || recipient == null || !(Friendship.alreadyFriendlyTransactional(sender, recipient))) {
            return this.errorResponse("You must be a friend of the recipient");
        }


        ObjectNode node = this.successResponseTemplate(WebSocketService.WS_METHOD_RECEIVE_CHAT);
        node.put("sender", Json.toJson(sender.getAsJson()));
        node.put("text", text);

        recipientActor.tell(Json.toJson(node), senderActor);

        return Json.toJson("OK");
    }

    /**
     * WebSocket method when receiving chat.
     *
     * @param wsMessage WebSocket message as JsonNode object
     * @param senderActor Sending actor
     * @param sender Sending account
     * @return WebSocket response
     */
    @SuppressWarnings("unused")
    private JsonNode wsReceiveChat(JsonNode wsMessage, ActorRef senderActor, Account sender) {
        return wsMessage;
    }

    /**
     * WebSocket method when receiving notification.
     *
     * @param wsMessage WebSocket message as JsonNode object
     * @param senderActor Sending actor
     * @param sender Sending account
     * @return WebSocket response
     */
    @SuppressWarnings("unused")
    private JsonNode wsReceiveNotification(JsonNode wsMessage, ActorRef senderActor, Account sender) {
        return wsMessage;
    }
}
