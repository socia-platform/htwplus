package managers;

import models.Account;
import models.Notification;
import models.base.BaseModel;
import models.enums.EmailNotifications;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Iven on 17.12.2015.
 */
public class NotificationManager implements BaseManager {


    @Inject
    JPAApi jpaApi;

    @Override
    public void create(Object object) {
        jpaApi.em().persist(object);
    }

    @Override
    public void update(Object object) {
        ((Notification) object).updatedAt();
        jpaApi.em().merge(object);
    }

    @Override
    public void delete(Object object) {
        jpaApi.em().remove(object);

    }

    /**
     * Returns a list of notifications by a specific user account ID.
     *
     * @param accountId     User account ID
     * @param maxResults    Maximum results
     * @param offsetResults Offset of results
     * @return List of notifications
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    public List<Notification> findByAccountId(final Long accountId, final int maxResults, final int offsetResults) throws Throwable {
        return (List<Notification>) jpaApi.em()
                .createQuery("FROM Notification n WHERE n.recipient.id = :accountId ORDER BY n.isRead ASC, n.updatedAt DESC")
                .setParameter("accountId", accountId)
                .setMaxResults(maxResults)
                .setFirstResult(offsetResults)
                .getResultList();
    }

    /**
     * Returns a list of notifications <b>sent</b> by a specific account ID.
     *
     * @param senderId User account ID
     * @return List of notifications
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    public List<Notification> findBySenderId(final Long senderId) {
        return (List<Notification>) jpaApi.em()
                .createQuery("FROM Notification n WHERE n.sender.id = :senderId")
                .setParameter("senderId", senderId)
                .getResultList();
    }

    /**
     * Returns a list of notifications by a specific user account ID for a specific page.
     *
     * @param accountId   User account ID
     * @param maxResults  Maximum results
     * @param currentPage Current page
     * @return List of notifications
     * @throws Throwable
     */
    public List<Notification> findByAccountIdForPage(final Long accountId, int maxResults, int currentPage) throws Throwable {
        return findByAccountId(accountId, maxResults, (currentPage * maxResults) - maxResults);
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
    public void deleteReferences(final BaseModel reference) {
        jpaApi.em().createQuery("DELETE FROM Notification n WHERE n.referenceId = :referenceId")
                .setParameter("referenceId", reference.id)
                .executeUpdate();
    }

    /**
     * Deletes a notification for a specific account ID containing a specific reference.
     *
     * @param reference BaseModel reference
     * @param accountId User account ID
     */
    public void deleteReferencesForAccountId(final BaseModel reference, final long accountId) {
        jpaApi.em().createQuery("DELETE FROM Notification n WHERE n.referenceId = :referenceId AND n.recipient.id = :accountId")
                .setParameter("referenceId", reference.id)
                .setParameter("accountId", accountId)
                .executeUpdate();
    }

    /**
     * Deletes all notifications for a specific account ID
     *
     * @param accountId User account ID
     */
    public void deleteNotificationsForAccount(final long accountId) {
        jpaApi.em().createQuery("DELETE FROM Notification n WHERE n.recipient.id = :accountId")
                .setParameter("accountId", accountId)
                .executeUpdate();
    }

    /**
     * Returns a specific notification by its ID.
     *
     * @param id Notification ID
     * @return Notification instance
     */
    public Notification findById(Long id) {
        return jpaApi.em().find(Notification.class, id);
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
        return JPA.createFor("defaultPersistenceUnit").withTransaction(() -> {
                return JPA.em().createQuery("FROM Notification n WHERE n.referenceId = :referenceId AND n.recipient.id = :recipientId", Notification.class)
                    .setParameter("referenceId", referenceId)
                    .setParameter("recipientId", recipientId)
                    .getSingleResult();
        });
    }

    /**
     * Returns a specific notification by its rendered content.
     *
     * @param renderedContent Rendered content to select
     * @return Notification instance
     */
    public List<Notification> findByRenderedContent(String renderedContent) throws NoResultException {
        return jpaApi.em()
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
    public int countNotificationsForAccountId(final Long accountId) {
        return ((Number) jpaApi.em()
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
        return ((Number) JPA.em()
                .createQuery("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :accountId AND n.isRead = false")
                .setParameter("accountId", accountId)
                .getSingleResult()).intValue();
    }

    /**
     * Returns a map with recipients as a key and a list of unsent and unread notifications
     * as value. The map contains recipients, who wish to receive either hourly emails or
     * daily, if the current hour is equal the desired receiving hour.
     * <p>
     * Example: {<Account>: <List<Notification>, <Account>: <List<Notification>, ...}
     *
     * @return Map of accounts containing list of unsent and unread notifications
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    public Map<Account, List<Notification>> findUsersWithDailyHourlyEmailNotifications() throws Throwable {
        Map<Account, List<Notification>> accountMap = new HashMap<>();
        List<Object[]> notificationsRecipients = findRecipients();

        // translate list of notifications and accounts into map
        for (Object[] entry : notificationsRecipients) {
            Notification notification = (Notification) entry[0];
            Account account = (Account) entry[1];
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

    @SuppressWarnings("unchecked")
    private List<Object[]> findRecipients() {
        return jpaApi.withTransaction(() -> {
            return jpaApi.em()
                    .createQuery("FROM Notification n JOIN n.recipient a WHERE n.isSent = false AND n.isRead = false "
                            + "AND ((a.emailNotifications = :daily AND HOUR(CURRENT_TIME) = a.dailyEmailNotificationHour) "
                            + "OR a.emailNotifications = :hourly) ORDER BY n.recipient.id DESC"
                    )
                    .setParameter("daily", EmailNotifications.COLLECTED_DAILY)
                    .setParameter("hourly", EmailNotifications.HOURLY)
                    .getResultList();
        });
    }

    /**
     * Marks all unread notifications as read for an account.
     *
     * @param account Account
     */
    public void markAllAsRead(Account account) {
        jpaApi.em()
                .createQuery("UPDATE Notification n SET n.isRead = true WHERE n.recipient = :account AND n.isRead = false")
                .setParameter("account", account)
                .executeUpdate();
    }
}
