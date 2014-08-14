package models;

import com.typesafe.plugin.MailerAPI;
import com.typesafe.plugin.MailerPlugin;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.libs.F;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles sending of emails, e.g. for notification mails.
 */
public class EmailHandler {
    static final String EMAIL_SENDER = Play.application().configuration().getString("htwplus.email.sender");
    static final String PLAIN_TEXT_TEMPLATE = "views.html.Emails.notificationsPlainText";
    static final String HTML_TEMPLATE = "views.html.Emails.notificationsHtml";

    /**
     * Singleton instance
     */
    private static EmailHandler instance = null;

    /**
     * Private constructor for singleton instance
     */
    private EmailHandler() { }

    /**
     * Returns the singleton instance.
     *
     * @return EmailHandler instance
     */
    public static EmailHandler getInstance() {
        if (EmailHandler.instance == null) {
            EmailHandler.instance = new EmailHandler();
        }

        return EmailHandler.instance;
    }

    /**
     * Determines the fully qualified path to the rendered template class,
     * invokes the static render() method and returns the rendered content.
     *
     * @param notifications List of notifications
     * @param recipient Recipient of the email notification
     * @param templatePath Template path
     * @return Rendered content
     * @throws Exception
     */
    protected String getRenderedNotification(List<NewNotification> notifications, Account recipient, String templatePath)
            throws Exception {
        Class<?> templateClass = Class.forName(templatePath);
        Class[] parameterClasses = { List.class, Integer.class, String.class };

        Method renderMethod = templateClass.getDeclaredMethod("render", parameterClasses);

        return renderMethod
                .invoke(null, notifications, notifications.size(), recipient.name)
                .toString();
    }

    /**
     * Sends an email.
     *
     * @param subject Subject of email
     * @param recipient Recipient of email
     * @param mailPlainText Plain text content of the mail (allowed to be null)
     * @param mailHtml HTML content of the mail (allowed to be null)
     */
    public void sendEmail(String subject, String recipient, String mailPlainText, String mailHtml) {
        MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
        mail.setSubject(subject);
        mail.setRecipient(recipient);
        mail.setFrom(EmailHandler.EMAIL_SENDER);

        // send email either in plain text, HTML or both
        if (mailPlainText != null) {
            if (mailHtml != null) {
                mail.send(mailPlainText, mailHtml);
            } else {
                mail.send(mailPlainText);
            }
        } else if (mailHtml != null) {
            mail.sendHtml(mailHtml);
        }
    }

    /**
     * Sends a list of notifications via email.
     *
     * @param notifications List of notifications
     * @param recipient Recipient of the email notification
     */
    public void sendNotificationsEmail(final List<NewNotification> notifications, Account recipient) {
        try {
            // send the email
            this.sendEmail(
                    Messages.get("notification.email_notifications.single.subject"),
                    recipient.name + " <" + recipient.email + ">",
                    this.getRenderedNotification(notifications, recipient, EmailHandler.PLAIN_TEXT_TEMPLATE),
                    this.getRenderedNotification(notifications, recipient, EmailHandler.HTML_TEMPLATE)
            );

            // mark notifications to be sent (JPA transaction required, as this is a async process)
            JPA.withTransaction(new F.Callback0() {
                @Override
                public void invoke() throws Throwable {
                    for (NewNotification notification : notifications) {
                        notification.isSent = true;
                        notification.update();
                    }
                }
            });

            // log information
            Logger.info("Successfully sent " + notifications.size() + " notification(s) to user " + recipient.id
                    + " via email."
            );
        } catch (Exception e) {
            Logger.error("Could not send notification(s) to recipient." + e.getMessage());
        }
    }

    /**
     * Sends a single notification via email.
     *
     * @param notification Notification instance
     */
    public void sendNotificationEmail(NewNotification notification) {
        List<NewNotification> notifications = new ArrayList<NewNotification>();
        notifications.add(notification);

        this.sendNotificationsEmail(notifications, notification.recipient);
    }
}
