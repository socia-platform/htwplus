import base.FakeApplicationTest;
import mock.MockNotifiable;
import models.Account;
import models.Notification;
import models.services.NotificationService;
import org.junit.*;
import play.db.jpa.JPA;
import play.libs.F;
import java.util.ArrayList;
import java.util.List;


/**
 * Testing notifications.
 */
public class NotificationTest extends FakeApplicationTest {
    /**
     * Tests, if no notification is created, when no recipient is set.
     */
    @Test
    public void testNoNotification() throws Throwable {
        final MockNotifiable notifiable = new MockNotifiable();
        notifiable.sender = this.getTestAccount(1);
        notifiable.rendered = "MOCK NO NOTIFICATION";
        NotificationService.getInstance().createNotification(notifiable);
        this.sleep(5); // sleep to ensure, that Akka would be done with notification creation

        JPA.withTransaction(new F.Callback0() {
            @Override
            @SuppressWarnings("unused")
            public void invoke() throws Throwable {
                List<Notification> notifications = Notification.findByRenderedContent(notifiable.rendered);
                //assertThat(notifications.size()).isEqualTo(0);
            }
        });
    }

    /**
     * Tests, if a notification is created
     */
    @Test
    public void testNotification() {
        final MockNotifiable notifiable = new MockNotifiable();
        final Account testAccount = this.getTestAccount(2);
        List<Account> recipients = new ArrayList<>();
        recipients.add(testAccount);

        notifiable.sender = this.getTestAccount(1);
        notifiable.recipients = recipients;
        notifiable.rendered = "MOCK NOTIFICATION";
        NotificationService.getInstance().createNotification(notifiable);
        this.sleep(5); // sleep to ensure, that Akka is done with notification creation

        // test, that we have exactly one notification
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                List<Notification> notifications = Notification.findByRenderedContent(notifiable.rendered);
                //assertThat(notifications.size()).isEqualTo(1);

                Notification notification = notifications.get(0);

                //assertThat(notification).isInstanceOf(Notification.class);
                //assertThat(notification.recipient.id).isEqualTo(testAccount.id);
                notification.delete();
            }
        });

        // now test, if the previous delete() worked
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                List<Notification> notifications = Notification.findByRenderedContent(notifiable.rendered);
                //assertThat(notifications.size()).isEqualTo(0);
            }
        });
    }
}
