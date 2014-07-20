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
        JPA.em().persist(this);
    }

    @Override
    public void delete() {
        JPA.em().remove(this);
    }

    /**
     * Returns a list of string containing already rendered content of notifications
     * by a specific user account.
     *
     * @param accountId User account ID
     * @return List of strings
     */
    @SuppressWarnings("unchecked")
    public static List<String> findRenderedContentByAccount(final Long accountId) throws Throwable {
        return JPA.withTransaction(new F.Function0<List<String>>() {
            @Override
            public List<String> apply() throws Throwable {
                return (List<String>) JPA.em()
                        .createQuery("SELECT n.rendered FROM NewNotification n WHERE n.recipient.id = :accountId ORDER BY n.updatedAt DESC")
                        .setParameter("accountId", accountId)
                        .getResultList();
            }
        });
    }
}
