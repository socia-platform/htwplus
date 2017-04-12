package models.services;

import managers.NotificationManager;
import play.Configuration;
import play.api.i18n.Lang;
import play.db.jpa.JPAApi;
import play.i18n.MessagesApi;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import javax.inject.Inject;
import javax.inject.Singleton;

import models.Account;
import models.Notification;
import play.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * This class handles sending of emails, e.g. for notification mails.
 */
@Singleton
public class EmailService {

    Configuration configuration;
    MailerClient mailerClient;
    Email email;
    NotificationManager notificationManager;
    JPAApi jpaApi;
    MessagesApi messagesApi;

    @Inject
    public EmailService(Configuration configuration,
                        MailerClient mailerClient,
                        Email email,
                        NotificationManager notificationManager,
                        JPAApi jpaApi,
                        MessagesApi messagesApi) {
        this.configuration = configuration;
        this.mailerClient = mailerClient;
        this.email = email;
        this.notificationManager = notificationManager;
        this.jpaApi = jpaApi;
        this.messagesApi = messagesApi;

        this.EMAIL_SENDER = configuration.getString("htwplus.email.sender");


    }

    private String EMAIL_SENDER;
    private String PLAIN_TEXT_TEMPLATE = "views.html.Emails.notificationsPlainText";
    private String HTML_TEMPLATE = "views.html.Emails.notificationsHtml";

    /**
     * Sends an email.
     *
     * @param subject Subject of email
     * @param recipient Recipient of email
     * @param mailPlainText Plain text content of the mail (allowed to be null)
     * @param mailHtml HTML content of the mail (allowed to be null)
     */
    public void sendEmail(String subject, String recipient, String mailPlainText, String mailHtml) {

        // since this instance is a singleton we need to ensure that there is no other recipient.
        // email.addTo adds further recipients so we use email.setTo to pass a list with just one recipient.
        List<String> recipientList = new ArrayList<>();
        recipientList.add(recipient);

        email.setSubject(subject);
        email.setTo(recipientList);
        email.setFrom(EMAIL_SENDER);

        // send email either in plain text, HTML or both
        if (mailPlainText != null) {
            if (mailHtml != null) {
                email.setBodyHtml(mailHtml);
                email.setBodyText(mailPlainText);
            } else {
                email.setBodyText(mailPlainText);
            }
        } else if (mailHtml != null) {
            email.setBodyHtml(mailHtml);
        }
        mailerClient.send(email);
    }

    /**
     * Sends a list of notifications via email.
     *
     * @param notifications List of notifications
     * @param recipient Recipient of the email notification
     */
    public void sendNotificationsEmail(final List<Notification> notifications, Account recipient) {
        try {
            String subject = notifications.size() > 1
                    ? messagesApi.get(Lang.defaultLang(), "notification.email_notifications.collected.subject", notifications.size())
                    : messagesApi.get(Lang.defaultLang(), "notification.email_notifications.single.subject_specific",
                        notifications.get(0).rendered.replaceAll("<[^>]*>", "").replaceAll("\\r?\\n|\\r", ", "));
            // send the email
            this.sendEmail(subject, recipient.name + " <" + recipient.email + ">",
                    TemplateService.getInstance().getRenderedTemplate(PLAIN_TEXT_TEMPLATE, notifications, recipient),
                    TemplateService.getInstance().getRenderedTemplate(HTML_TEMPLATE, notifications, recipient)
            );

            // mark notifications to be sent (JPA transaction required, as this is a async process)
            jpaApi.withTransaction(() -> {
                for (Notification notification : notifications) {
                    notification.isSent = true;
                    notificationManager.update(notification);
                }
            });

            // log information
            Logger.info("Successfully sent " + notifications.size() + " notification(s) to user " + recipient.id
                    + " via email."
            );
        } catch (Exception e) {
            Logger.error("Could not send notification(s) to recipient. " + e.getMessage());
        }
    }

    /**
     * Sends a single notification via email.
     *
     * @param notification Notification instance
     */
    public void sendNotificationEmail(Notification notification) {
        List<Notification> notifications = new ArrayList<>();
        notifications.add(notification);

        this.sendNotificationsEmail(notifications, notification.recipient);
    }

    /**
     * Finds all users, who wants to receive daily or hourly notifications and sends them one
     * email with all the new notifications (if any).
     */
    public void sendDailyHourlyNotificationsEmails() {
        try {
            Logger.info("Start sending of hourly email notifications...");

            // load map with recipients containing list of unread notifications and iterate over the map

            Map<Account, List<Notification>> notificationsRecipients = notificationManager.findUsersWithDailyHourlyEmailNotifications();
            for (Map.Entry<Account, List<Notification>> entry : notificationsRecipients.entrySet()) {
                this.sendNotificationsEmail(entry.getValue(), entry.getKey());
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
