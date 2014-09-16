package controllers;

import java.util.LinkedList;
import java.util.List;

import models.Account;
import models.NewNotification;
import play.Play;
import play.twirl.api.Html;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Notification.view;

@Transactional
@Security.Authenticated(Secured.class)
public class NotificationController extends BaseController {
    static final int LIMIT = Integer.parseInt(Play.application().configuration().getString("htwplus.notification.limit"));

    /**
     * @deprecated Deprecated since refactor of notification system
     * @return Html
     */
	public static Html view() {
		return getNotifications();
	}

    /**
     * Returns all notifications for current user.
     *
     * @return Html rendered instance
     */
    @Transactional(readOnly = true)
	private static Html getNotifications() {
		Account account = Component.currentAccount();
		
		if (account == null) {
			return new Html("Das wird nichts");
		}

        List<NewNotification> list = null;
        try {
            list = NewNotification.findByAccountId(account.id);
        } catch (Throwable throwable) { throwable.printStackTrace(); }

        List<Integer> countedNotifications = NotificationController.countNotifications(list);

        return views.html.Notification.menuitem.render(list, countedNotifications.get(0), countedNotifications.get(1));
	}

    /**
     * Redirects to the target URL of the notification, if user has access.
     *
     * @param notificationId ID of the notification
     * @return SimpleResult with redirection
     */
    @Transactional
	public static Result forward(Long notificationId) {
		NewNotification notification = NewNotification.findById(notificationId);

		if (notification == null) {
			return badRequest("Das gibts doch garnicht!");
		}
		
		if (!Secured.hasAccessToNotification(notification)) {
			return redirect(controllers.routes.Application.index());
		}

        notification.isRead = true;
        notification.update();

		return redirect(notification.targetUrl);
	}

    /**
     * Shows all notifications.
     *
     * @param page Current page
     * @return Result
     */
    @Transactional(readOnly = true)
    public static Result showAll(int page) {
        List<NewNotification> notifications = null;
        try {
            notifications = NewNotification.findByAccountIdForPage(Component.currentAccount().id, NotificationController.LIMIT, page);
        } catch (Throwable throwable) { throwable.printStackTrace(); }

        Navigation.set(Navigation.Level.NOTIFICATIONS, Messages.get("notification.news"));
        return ok(view.render(notifications, LIMIT, page, NewNotification.countNotificationsForAccountId(Component.currentAccount().id)));
    }

    /**
     * Loops over a notification list and counts:
     * 1st element (index 0): All notifications
     * 2nd element (index 1): Unread notifications
     * 3rd element (index 2): Read notifications
     *
     * @param notifications List of notifications
     * @return List with count of all, unread and read notifications
     */
    public static List<Integer> countNotifications(List<NewNotification> notifications) {
        int countAll = 0;
        int countUnread = 0;
        int countRead = 0;
        List<Integer> count = new LinkedList<>();

        if (notifications != null) {
            countAll = notifications.size();
            for (NewNotification notification : notifications) {
                if (notification.isRead) {
                    countRead++;
                }
            }
            countUnread = countAll - countRead;
        }

        // add counted elements to list
        count.add(0, countAll);
        count.add(1, countUnread);
        count.add(2, countRead);

        return count;
    }
}
