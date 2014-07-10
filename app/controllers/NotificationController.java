package controllers;

import java.util.ArrayList;
import java.util.List;

import models.Account;
import models.Notification;
import play.Logger;
import play.api.templates.Html;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import scala.collection.mutable.StringBuilder;

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
        return ok(NotificationController.getNotifications(true));
    }

    private static Html getNotifications() {
        return NotificationController.getNotifications(false);
    }
	
	private static Html getNotifications(boolean inclHistory) {
		Account account = Component.currentAccount();
		
		if (account == null) {
			return new Html(new StringBuilder("Das wird nichts"));
		}
		
		List<Notification> list = new ArrayList<Notification>();
		if (Notification.countForAccount(account) >= 0) {
            // retrieve list, if inclHistory then with history, else only unread notifications
			list = inclHistory ? Notification.findByUser(account) : Notification.findUnreadByUser(account);
		} 
		return views.html.Notification.menuitem.render(list, inclHistory);
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
	
	
	
}
