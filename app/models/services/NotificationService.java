package models.services;

import models.Account;
import models.NewNotification;
import models.base.BaseNotifiable;
import models.base.INotifiable;
import models.enums.EmailNotifications;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.Akka;
import play.libs.F;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the notification system.
 */
public class NotificationService {
    /**
     * Singleton instance
     */
    private static NotificationService instance = null;

    /**
     * Private constructor for singleton instance
     */
    private NotificationService() { }

    /**
     * Returns the singleton instance.
     *
     * @return NotificationHandler instance
     */
    public static NotificationService getInstance() {
        if (NotificationService.instance == null) {
            NotificationService.instance = new NotificationService();
        }

        return NotificationService.instance;
    }

    /**
     * Creates one or more notifications by the notifiable instance.
     * The creation is done asynchronized using the Akka subsystem to be non-blocking.
     *
     * @param notifiable Notifiable instance, to retrieve the required notification data
     */
    public void createNotification(final INotifiable notifiable) {
        // schedule async process in 0 second from now on
        Akka.system().scheduler().scheduleOnce(
            Duration.create(0, TimeUnit.SECONDS),
            new Runnable() {
                // runs the Akka schedule
                public void run() {
                    List<Account> recipients = notifiable.getRecipients();

                    // if no recipients, abort
                    if (recipients == null || recipients.size() == 0) {
                        return;
                    }

                    // run through all recipients
                    for (Account recipient : recipients) {
                        // if sender == recipient, it is not necessary to create a notification -> continue
                        if (recipient.equals(notifiable.getSender())) {
                            continue;
                        }

                        // create new notification and persist in database
                        final NewNotification notification = new NewNotification();
                        notification.isRead = false;
                        notification.isSent = false;
                        notification.recipient = recipient;
                        notification.sender = notifiable.getSender();
                        notification.reference = notifiable.getReference();
                        notification.targetUrl = notifiable.getTargetUrl();

                        try {
                            notification.rendered = notifiable.render(notification);

                            // persist notification using JPA.withTransaction, as we are not in the main
                            // execution context of play, but in Akka sub-system
                            JPA.withTransaction(new F.Callback0() {
                                @Override
                                public void invoke() throws Throwable {
                                    notification.create();
                                }
                            });

                            Logger.info("Created new async Notification for User: " + recipient.id.toString());
                            this.handleMail(notification);
                        } catch (Exception e) {
                            Logger.error("Could not render notification. Notification will not be stored in DB" +
                                            " nor will the user be notified in any way." + e.getMessage()
                            );
                        }
                    }
                }

                // sends mail to recipient, if he wishes to be notified via mail immediately
                // and notification is currently unsent/unread
                protected void handleMail(final NewNotification notification) {
                    if (notification.recipient.emailNotifications == EmailNotifications.IMMEDIATELY_ALL
                            && !notification.isSent
                            && !notification.isRead
                    ) {
                        // schedule another for email handling async process in 0 second from now on
                        Akka.system().scheduler().scheduleOnce(
                            Duration.create(0, TimeUnit.SECONDS),
                            new Runnable() {
                                // runs the Akka schedule
                                public void run() {
                                    EmailService.getInstance().sendNotificationEmail(notification);
                                }
                            },
                            Akka.system().dispatcher()
                        );
                    }
                }
            },
            Akka.system().dispatcher()
        );
    }

    /**
     * Overloaded method createNotification() with notification type. The notification type
     * is an important information for nearly every notification, as it determines the
     * correct template and eventually also logic in methods like getRecipients() of the
     * notification.
     *
     * @param notifiable Notifiable instance, to retrieve the required notification data
     * @param notificationType Type of this notification
     */
    public void createNotification(INotifiable notifiable, final String notificationType) {
        if (notifiable instanceof BaseNotifiable) {
            ((BaseNotifiable)notifiable).type = notificationType;
        }

        this.createNotification(notifiable);
    }
}
