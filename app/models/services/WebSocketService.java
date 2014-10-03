package models.services;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Account;
import models.Friendship;
import models.actors.WebSocketActor;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;

import java.lang.reflect.Method;
import java.util.*;

/**
 * WebSocket service that is handling all active WebSocket actors.
 */
@SuppressWarnings("unused")
public class WebSocketService {
    public static final String WS_METHOD_SEND_CHAT = "SendChat";
    public static final String WS_METHOD_RECEIVE_CHAT = "ReceiveChat";
    public static final String WS_METHOD_RECEIVE_NOTIFICATION = "ReceiveNotification";
    public static final String WS_METHOD_RECEIVE_PING = "Ping";
    public static final String WS_RESPONSE_OK = "OK";
    public static final String WS_RESPONSE_ERROR = "ERROR";

    /**
     * Singleton instance
     */
    private static WebSocketService instance = null;

    /**
     * Holds all active WebSocket actors per Account ID
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
     * @param account Account
     * @param in WebSocket input stream
     * @param out WebSocket output stream
     */
    public void invokeActor(final Account account, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        if (this.getActorForAccount(account) != null) {
            return;
        }

        this.accountActor.put(account.id, Akka.system().actorOf(Props.create(WebSocketActor.class, account, in, out)));
    }

    /**
     * Returns an ActorRef instance for an account if available, otherwise null.
     *
     * @param account Account
     * @return ActorRef instance if available for account ID, otherwise null
     */
    public ActorRef getActorForAccount(Account account) {
        return this.getActorForAccountId(account.id);
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
     * @param account Account
     * @param in WebSocket input stream
     * @param out WebSocket output stream
     * @return ActorRef instance
     */
    public ActorRef getActorForAccount(Account account, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        if (this.getActorForAccount(account) == null) {
            this.invokeActor(account, in, out);
        }

        return this.getActorForAccount(account);
    }

    /**
     * Stops an ActorRef for an account ID.
     *
     * @param account Account
     */
    public void stopActor(Account account) {
        if (this.getActorForAccount(account) == null) {
            return;
        }

        ActorRef stoppingActorRef = this.accountActor.remove(account.id);
        Akka.system().stop(stoppingActorRef);
    }

    /**
     * Handles a WebSocket message
     *
     * @param account Account
     * @param wsMessage WebSocket message as JsonNode object
     */
    public void handleWsMessage(Account account, JsonNode wsMessage) {
        // Log events to the console
        Logger.info("[WS] Received (User ID: " + account.id + "): " + wsMessage.toString());

        if (!wsMessage.has("method")) {
            Logger.error("[WS] No method received");
            return;
        }

        ActorRef senderActor = this.getActorForAccount(account);
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
        Map<String, Object> map = new HashMap<>();
        map.put("method", method);
        map.put("code", WebSocketService.WS_RESPONSE_OK);
        map.put("time", new Date());

        return JsonService.getInstance().getObjectNodeFromMap(map);
    }

    /**
     * Returns an account by ID, enclosed by transaction.
     *
     * @param accountId Account ID
     * @return Account
     */
    private Account getAccountById(final Long accountId) {
        try {
            return JPA.withTransaction(new F.Function0<Account>() {
                @Override
                public Account apply() throws Throwable {
                    return Account.findById(accountId);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    /**
     * Returns true, if Account A and Account B are friends.
     *
     * @param a Account A
     * @param b Account B
     * @return True, if friendship is established
     */
    private boolean isFriendshipEstablished(final Account a, final Account b) {
        try {
            return JPA.withTransaction(new F.Function0<Boolean>() {
                @Override
                public Boolean apply() throws Throwable {
                    return Friendship.alreadyFriendly(a, b);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
    }

    /**
     * WebSocket method Ping for testing purposes.
     *
     * @param wsMessage WebSocket message as JsonNode object
     * @param senderActor Sending actor
     * @param sender Sending account
     * @return WebSocket response
     */
    private JsonNode wsPing(JsonNode wsMessage, ActorRef senderActor, Account sender) {
        return Json.toJson("Pong");
    }

    /**
     * WebSocket method when sending chat.
     *
     * @param wsMessage WebSocket message as JsonNode object
     * @param senderActor Sending actor
     * @param sender Sending account
     * @return WebSocket response
     */
    private JsonNode wsSendChat(JsonNode wsMessage, ActorRef senderActor, Account sender) {
        // validate given parameters
        if (!wsMessage.has("recipient") || !wsMessage.has("text")) {
            return this.errorResponse("Could not send chat message, either no \"recipient\" or \"text\" information.");
        }

        Long recipientAccountId = wsMessage.get("recipient").asLong();
        String text = wsMessage.get("text").asText();
        Account recipient = this.getAccountById(recipientAccountId);

        // check, if recipient exists
        if (recipient == null) {
            return this.errorResponse("User not found");
        }


        ActorRef recipientActor = this.getActorForAccount(recipient);
        // check, if the recipient is online
        if (recipientActor == null) {
            return this.errorResponse("Recipient not online");
        }

        // sender should not be recipient
        if (recipientActor.equals(senderActor)) {
            return this.errorResponse("Cannot send chat to yourself");
        }
        // check, if sender and recipient are friends
        if (sender == null || !this.isFriendshipEstablished(sender, recipient)) {
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
    private JsonNode wsReceiveNotification(JsonNode wsMessage, ActorRef senderActor, Account sender) {
        return wsMessage;
    }
}
