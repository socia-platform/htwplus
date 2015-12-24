package models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.base.BaseModel;
import models.base.IJsonNodeSerializable;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import play.libs.Json;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Notification class as entity. Will replace the current Notification class in future.
 */
@Entity
@Table
public class Notification extends BaseModel implements IJsonNodeSerializable {
    /**
     * The sender of this notification.
     */
    @Required
    @OneToOne
    public Account sender;

    /**
     * The recipient of this notification.
     */
    @Required
    @OneToOne
    public Account recipient;

    @Column(name = "rendered")
    public String rendered;

    /**
     * True, if this notification is read by its recipient.
     */
    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    public boolean isRead;

    /**
     * True, if this notification is sent already via email.
     */
    @Column(name = "is_sent", nullable = false, columnDefinition = "boolean default false")
    public boolean isSent;

    /**
     * An object id, this notification has a reference to (e.g. when notified after posting the post).
     */
    @Column(name = "reference_id", nullable = false)
    public Long referenceId;

    /**
     * An object type, this notification has a reference to (e.g. when notified after posting the post).
     */
    @Column(name = "reference_type", nullable = false)
    public String referenceType;

    /**
     * The target URL, this notification refers to.
     */
    @Column(name = "target_url")
    public String targetUrl;

    @Override
    public ObjectNode getAsJson() {
        ObjectNode node = Json.newObject();
        node.put("id", this.id);
        node.put("is_read", this.isRead);
        node.put("content", this.rendered);
        node.put("created", this.createdAt.getTime());
        node.put("updated", this.updatedAt.getTime());

        return node;
    }

    /**
     * Marks all unread notifications as read for an account.
     *
     * @param account Account
     */
    public static void markAllAsRead(Account account) {
        JPA.em()
                .createQuery("UPDATE Notification n SET n.isRead = true WHERE n.recipient = :account AND n.isRead = false")
                .setParameter("account", account)
                .executeUpdate();
    }
}
