package controllers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import models.Account;
import models.NewNotification;
import models.Notification;
import models.actors.NewNotificationActor;
import play.Logger;
import play.api.templates.Html;
import play.db.jpa.Transactional;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.WebSocket;
import scala.collection.mutable.StringBuilder;
import scala.concurrent.duration.Duration;

@Transactional
@Security.Authenticated(Secured.class)
public class NotificationController extends BaseController{
	
	public static Html view() {
		return getNotifications();
	}
	
	public static Result viewAjax() {
		return ok(NotificationController.getNotifications());
	}

    public static Result viewHistoryAjax() {
        return ok(NotificationController.getNotifications());
    }
	
	private static Html getNotifications() {
		Account account = Component.currentAccount();
		
		if (account == null) {
			return new Html(new StringBuilder("Das wird nichts"));
		}

        List<NewNotification> list = null;
        try {
            list = NewNotification.findByAccount(account.id);
        } catch (Throwable throwable) { throwable.printStackTrace(); }

        return views.html.Notification.menuitem.render(list);
	}
	
	public static Result forward(Long notificationId, String url) {
		Notification note = Notification.findById(notificationId);
		Logger.info(url);

		if (note == null) {
			return badRequest("Das gibts doch garnicht!");
		}
		
		if (!Secured.deleteNotification(note)) {
			return redirect(controllers.routes.Application.index());
		}

        note.notificationRead = true;
        note.update();

		return redirect(url);
	}
	
	public static Result deleteAll() {
        Notification.markAllAsReadByUser(Component.currentAccount());
        flash("success", "Alle Neuigkeiten wurden als gelesen markiert.");
		return ok();
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

    /**
     * Handles the web socket channel by invoking Akka actor that writes notifications to the web socket.
     *
     * @return Web socket instance including JSON nodes
     */
    public static WebSocket<JsonNode> wsNotifications() {
        final Long accountId = NotificationController.getCurrentAccountId();

        // called when the Websocket Handshake is done.
        return new WebSocket<JsonNode>() {
            @Override
            public void onReady(In<JsonNode> in, Out<JsonNode> out) {
                if (accountId == null) {
                    out.close();
                }

                final ActorRef newNotificationActor = Akka.system().actorOf(Props.create(NewNotificationActor.class, in, out, accountId));
                final Cancellable cancellable = Akka.system().scheduler().schedule(
                        Duration.create(20, TimeUnit.SECONDS),
                        Duration.create(20, TimeUnit.SECONDS),
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
