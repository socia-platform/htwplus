package models;

import models.base.BaseModel;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import play.libs.F;

import javax.persistence.*;
import java.util.List;

/**
 * Notification class as entity. Will replace the current Notification class in future.
 */
@Entity
@Table
public class NewNotification extends BaseModel {
    @Required
    @OneToOne
    public Account sender;

    @Required
    @OneToOne
    public Account recipient;

    @Column(name = "rendered")
    public String rendered;

    @Column(name = "is_read")
    public boolean isRead;

    @ManyToOne
    public BaseModel reference;

    @Column(name = "target_url")
    public String targetUrl;

    @Override
    public void create() {
        JPA.em().persist(this);
    }

    @Override
    public void update() {
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
    public static List<NewNotification> findByAccountId(final Long accountId, final int maxResults, final int offsetResults) throws Throwable {
        return JPA.withTransaction(new F.Function0<List<NewNotification>>() {
            @Override
            public List<NewNotification> apply() throws Throwable {
                return (List<NewNotification>) JPA.em()
                        .createQuery("FROM NewNotification n WHERE n.recipient.id = :accountId ORDER BY n.createdAt DESC")
                        .setParameter("accountId", accountId)
                        .setMaxResults(maxResults)
                        .setFirstResult(offsetResults)
                        .getResultList();
            }
        });
    }

    /**
     * Overloaded method findByAccountId() with default offset 0
     *
     * @param accountId User account ID
     * @return List of notifications
     * @throws Throwable
     */
    public static List<NewNotification> findByAccountId(final Long accountId, final int maxResults) throws Throwable {
        return NewNotification.findByAccountId(accountId, maxResults, 0);
    }

    /**
     * Overloaded method findByAccountId() with default max results of 10 and offset 0
     *
     * @param accountId User account ID
     * @return List of notifications
     * @throws Throwable
     */
    public static List<NewNotification> findByAccountId(final Long accountId) throws Throwable {
        return NewNotification.findByAccountId(accountId, 10);
    }

    /**
     * Returns a list of notifications by a specific user account ID for a specific page.
     *
     * @param accountId User account ID
     * @param currentPage Current page
     * @return List of notifications
     * @throws Throwable
     */
    public static List<NewNotification> findByAccountIdForPage(final Long accountId, int maxResults, int currentPage) throws Throwable {
        return NewNotification.findByAccountId(accountId, maxResults, (currentPage * maxResults) - maxResults);
    }

    /**
     * Deletes a notification with containing a specific reference.
     *
     * @param reference BaseModel reference
     */
    public static void deleteReferences(final BaseModel reference) {
        JPA.em().createQuery("DELETE FROM NewNotification n WHERE n.reference = :reference")
                .setParameter("reference", reference)
                .executeUpdate();
    }

    /**
     * Returns a specific notification by its ID.
     *
     * @param id Notification ID
     * @return Notification instance
     */
    public static NewNotification findById(Long id) {
        return JPA.em().find(NewNotification.class, id);
    }
}
