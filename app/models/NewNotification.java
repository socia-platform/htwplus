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
     * Returns a list of notifications by a specific user account.
     *
     * @param accountId User account ID
     * @param maxResults Maximum results
     * @return List of notifications
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    public static List<NewNotification> findByAccount(final Long accountId, final int maxResults) throws Throwable {
        return JPA.withTransaction(new F.Function0<List<NewNotification>>() {
            @Override
            public List<NewNotification> apply() throws Throwable {
                return (List<NewNotification>) JPA.em()
                        .createQuery("FROM NewNotification n WHERE n.recipient.id = :accountId ORDER BY n.updatedAt DESC")
                        .setParameter("accountId", accountId)
                        .setMaxResults(maxResults)
                        .getResultList();
            }
        });
    }

    /**
     * Overloaded method findByAccount() with default max results of 10
     *
     * @param accountId User account ID
     * @return List of notifications
     * @throws Throwable
     */
    public static List<NewNotification> findByAccount(final Long accountId) throws Throwable {
        return NewNotification.findByAccount(accountId, 10);
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
}
