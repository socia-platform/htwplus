package models.base;

import models.Account;
import models.NewNotification;

import java.util.List;

/**
 * Interface for objects, that are notifiable (like post, friendship, group, etc.)
 */
public interface INotifiable {
    /**
     * Returns the sender of this notification.
     *
     * @return Sender account instance
     */
    public Account getSender();

    /**
     * Returns a list of recipients for this notification.
     *
     * @return List of recipients
     */
    public List<Account> getRecipients();

    /**
     * Pre-Renders this notification, to be saved into the DB and be accessed instantly by the notification handler.
     *
     * @param notification The notification which is rendered
     * @return Rendered HTML
     */
    public String render(NewNotification notification);

    /**
     * Returns the reference, this notification is about (e.g. when posting the actual Post)
     *
     * @return BaseModel of the reference
     */
    public BaseModel getReference();
}
