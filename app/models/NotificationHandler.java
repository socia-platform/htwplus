package models;

import models.base.INotifiable;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.Akka;
import play.libs.F;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * This class handles the notification system.
 */
public class NotificationHandler {
    /**
     * Singleton instance
     */
    private static NotificationHandler instance = null;

    /**
     * Private constructor for singleton instance
     */
    private NotificationHandler() { }

    /**
     * Returns the singleton instance.
     *
     * @return NotificationHandler instance
     */
    public static NotificationHandler getInstance() {
        if (NotificationHandler.instance == null) {
            NotificationHandler.instance = new NotificationHandler();
        }

        return NotificationHandler.instance;
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
                        // run through all recipients
                        for (Account recipient : notifiable.getRecipients()) {
                            // if sender == recipient, it is not necessary to create a notification -> continue
                            if (recipient.equals(notifiable.getSender())) {
                                continue;
                            }

                            // create new notification and persist in database
                            final NewNotification notification = new NewNotification();
                            notification.isRead = false;
                            notification.recipient = recipient;
                            notification.sender = notifiable.getSender();
                            notification.reference = notifiable.getReference();
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
                        }
                    }
                    // sends mail to recipient, if he wishes to be notified by mail
                    protected void handleMail(NewNotification notification) {
                        // TODO
                    }
                },
                Akka.system().dispatcher()
        );
    }
}
