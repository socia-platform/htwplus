package models.actors;

import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import models.Account;
import models.services.WebSocketService;
import play.mvc.WebSocket;

import javax.inject.Inject;

/**
 * WebSocket actor to handle WebSocket communication asynchronously.
 */
public class WebSocketActor extends UntypedActor {
    Account account;
    WebSocketService webSocketService;
    WebSocket.In<JsonNode> in;
    WebSocket.Out<JsonNode> out;

    @Inject
    public WebSocketActor(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    /**
     * Constructor, sets the web socket attributes.
     *
     * @param account Current user
     * @param in WebSocket input stream
     * @param out WebSocket output stream
     */
    public WebSocketActor(Account account, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        this.account = account;
        this.in = in;
        this.out = out;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        try {
            // write response to WebSocket channel by invoking WebSocket method with given
            // input WebSocket data parameters and the ActorRef of this
            this.out.write(webSocketService.invokeWsMethod((JsonNode) message, this.self(), this.account));
        } catch (Throwable ex) {
            unhandled(ex.getMessage());
            this.context().stop(this.self());
        }
    }
}
