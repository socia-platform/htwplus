package models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.base.BaseModel;
import models.base.IJsonNodeSerializable;
import models.enums.EmailNotifications;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import play.libs.F;
import play.libs.Json;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void create() {
        JPA.em().persist(this);
    }

    @Override
    public void update() {
        updatedAt();
        JPA.em().merge(this);
    }

    @Override
    public void delete() {
        JPA.em().remove(this);
    }

    /**
     * Returns a list of notifications by a specific user account ID.
     *
     * @param accountId User account ID
     * @param maxResults Maximum results
     * @param offsetResults Offset of results
     * @return List of notifications
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    public static List<Notification> findByAccountId(final Long accountId, final int maxResults, final int offsetResults) throws Throwable {
    	return (List<Notification>) JPA.em()
                .createQuery("FROM Notification n WHERE n.recipient.id = :accountId ORDER BY n.isRead ASC, n.updatedAt DESC")
                .setParameter("accountId", accountId)
                .setMaxResults(maxResults)
                .setFirstResult(offsetResults)
                .getResultList();
    }

    /**
     * Returns a list of notifications by a specific user account ID for a specific page.
     *
     * @param accountId User account ID
     * @param maxResults Maximum results
     * @param currentPage Current page
     * @return List of notifications
     * @throws Throwable
     */
    public static List<Notification> findByAccountIdForPage(final Long accountId, int maxResults, int currentPage) throws Throwable {
        return Notification.findByAccountId(accountId, maxResults, (currentPage * maxResults) - maxResults);
    }

    /**
     * Returns a list of unread notifications by a specific user account ID.
     *
     * @param accountId User account ID
     * @return List of notifications
     * @throws Throwable
     */
    public static List<Notification> findByAccountIdUnread(final Long accountId) throws Throwable {
        return JPA.em()
                .createQuery("FROM Notification n WHERE n.recipient.id = :accountId AND n.isRead = false", Notification.class)
                .setParameter("accountId", accountId)
                .setMaxResults(10)
                .getResultList();
    }

    /**
     * Deletes a notification with containing a specific reference.
     *
     * @param reference BaseModel reference
     */
    public static void deleteReferences(final BaseModel reference) {
        JPA.em().createQuery("DELETE FROM Notification n WHERE n.referenceId = :referenceId")
                .setParameter("referenceId", reference.id)
                .executeUpdate();
    }

    /**
     * Deletes a notification for a specific account ID containing a specific reference.
     *
     * @param reference BaseModel reference
     * @param accountId User account ID
     */
    public static void deleteReferencesForAccountId(final BaseModel reference, final long accountId) {
        JPA.em().createQuery("DELETE FROM Notification n WHERE n.referenceId = :referenceId AND n.recipient.id = :accountId")
                .setParameter("referenceId", reference.id)
                .setParameter("accountId", accountId)
                .executeUpdate();
    }

    /**
     * Returns a specific notification by its ID.
     *
     * @param id Notification ID
     * @return Notification instance
     */
    public static Notification findById(Long id) {
        return JPA.em().find(Notification.class, id);
    }

    /**
     * Returns a notification by a reference ID and a recipient ID.
     *
     * @param referenceId Reference ID
     * @param recipientId Recipient ID
     * @return Notification instance
     * @throws NoResultException
     */
    public static Notification findByReferenceIdAndRecipientId(Long referenceId, Long recipientId) throws NoResultException {
        return JPA.em()
                .createQuery("FROM Notification n WHERE n.referenceId = :referenceId AND n.recipient.id = :recipientId", Notification.class)
                .setParameter("referenceId", referenceId)
                .setParameter("recipientId", recipientId)
                .getSingleResult();
    }

    /**
     * Returns a specific notification by its rendered content.
     *
     * @param renderedContent Rendered content to select
     * @return Notification instance
     */
    public static List<Notification> findByRenderedContent(String renderedContent) throws NoResultException {
        return JPA.em()
                .createQuery("FROM Notification n WHERE n.rendered = :renderedContent", Notification.class)
                .setParameter("renderedContent", renderedContent)
                .getResultList();
    }

    /**
     * Counts all notifications for an account ID.
     *
     * @param accountId User account ID
     * @return Number of notifications
     */
    public static int countNotificationsForAccountId(final Long accountId) {
    	return ((Number)JPA.em()
                .createQuery("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :accountId")
                .setParameter("accountId", accountId)
                .getSingleResult()).intValue();
    }

    /**
     * Counts all unread notifications for an account ID.
     *
     * @param accountId User account ID
     * @return Number of notifications
     */
    public static int countUnreadNotificationsForAccountId(final Long accountId) {
        return ((Number)JPA.em()
                .createQuery("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :accountId AND n.isRead = false")
                .setParameter("accountId", accountId)
                .getSingleResult()).intValue();
    }

    /**
     * Returns a map with recipients as a key and a list of unsent and unread notifications
     * as value. The map contains recipients, who wish to receive either hourly emails or
     * daily, if the current hour is equal the desired receiving hour.
     *
     * Example: {<Account>: <List<Notification>, <Account>: <List<Notification>, ...}
     *
     * @return Map of accounts containing list of unsent and unread notifications
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    public static Map<Account, List<Notification>> findUsersWithDailyHourlyEmailNotifications() throws Throwable {
        List<Object[]> notificationsRecipients = JPA.withTransaction(new F.Function0<List<Object[]>>() {
            @Override
            public List<Object[]> apply() throws Throwable {
                return (List<Object[]>) JPA.em()
                    .createQuery("FROM Notification n JOIN n.recipient a WHERE n.isSent = false AND n.isRead = false "
                        + "AND ((a.emailNotifications = :daily AND HOUR(CURRENT_TIME) = a.dailyEmailNotificationHour) "
                        + "OR a.emailNotifications = :hourly) ORDER BY n.recipient.id DESC"
                    )
                    .setParameter("daily", EmailNotifications.COLLECTED_DAILY)
                    .setParameter("hourly", EmailNotifications.HOURLY)
                    .getResultList();
            }
        });

        // translate list of notifications and accounts into map
        Map<Account, List<Notification>> accountMap = new HashMap<>();
        for (Object[] entry : notificationsRecipients) {
            Notification notification = (Notification)entry[0];
            Account account = (Account)entry[1];
            List<Notification> listForAccount;

            // add account and new list of notifications, if not set already, otherwise load list
            if (!accountMap.containsKey(account)) {
                listForAccount = new ArrayList<>();
                accountMap.put(account, listForAccount);
            } else {
                listForAccount = accountMap.get(account);
            }

            // add notification to list for account
            listForAccount.add(notification);
        }

        return accountMap;
    }

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
