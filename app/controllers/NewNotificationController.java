package controllers;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import models.actors.NewNotificationActor;
import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Security;
import play.mvc.WebSocket;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

@Security.Authenticated(Secured.class)
public class NewNotificationController extends BaseController {
    private static Long getCurrentAccountId() {
        try {
            return Long.valueOf(Http.Context.current().session().get("id"));
        } catch (Exception ex) {
            return null;
        }
    }

    public static WebSocket<JsonNode> notifications() {
        final Long accountId = NewNotificationController.getCurrentAccountId();

        // called when the Websocket Handshake is done.
        return new WebSocket<JsonNode>() {
            @Override
            public void onReady(In<JsonNode> in, Out<JsonNode> out) {
                if (accountId == null) {
                    out.close();
                }

                final ActorRef newNotificationActor = Akka.system().actorOf(Props.create(NewNotificationActor.class, in, out, accountId));
                final Cancellable cancellable = Akka.system().scheduler().schedule(
                        Duration.create(10, TimeUnit.SECONDS),
                        Duration.create(10, TimeUnit.SECONDS),
                        newNotificationActor,
                        "getNotifications",
                        Akka.system().dispatcher(),
                        null
                );

                // For each event received on the socket,
                in.onMessage(new F.Callback<JsonNode>() {
                    public void invoke(JsonNode wsMessage) {
                        // Log events to the console
                        Logger.info(wsMessage.asText());
                    }
                });

                // When the socket is closed.
                in.onClose(new F.Callback0() {
                    public void invoke() {
                        cancellable.cancel();
                        Logger.info("Disconnected user ID: " + accountId);
                    }
                });
            }
        };
    }
}