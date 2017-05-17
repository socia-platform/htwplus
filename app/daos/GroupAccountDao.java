package daos;

import models.Account;
import models.Group;
import models.GroupAccount;
import models.enums.GroupType;
import models.enums.LinkType;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Created by Iven on 08.05.2017.
 */
public class GroupAccountDao {

    JPAApi jpaApi;

    @Inject
    public GroupAccountDao(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public GroupAccount findById(Long id) {
        return jpaApi.em().find(GroupAccount.class, id);
    }

    /**
     * Find all groups and courses where given account is owner or member
     */
    public List<Group> findEstablished(Account account) {
        @SuppressWarnings("unchecked")
        List<Group> groupAccounts = jpaApi
                .em()
                .createQuery(
                        "SELECT ga.group FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.linkType = ?2")
                .setParameter(1, account.id)
                .setParameter(2, LinkType.establish).getResultList();
        return groupAccounts;
    }

    /**
     * Find all groups where given account is owner or member
     */
    public List<Group> findGroupsEstablished(Account account) {
        @SuppressWarnings("unchecked")
        List<Group> groupAccounts = jpaApi
                .em()
                .createQuery(
                        "SELECT ga.group FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.linkType = ?2 AND ga.group.groupType != ?3 ORDER BY ga.group.title ASC")
                .setParameter(1, account.id)
                .setParameter(2, LinkType.establish)
                .setParameter(3, GroupType.course).getResultList();
        return groupAccounts;
    }

    /**
     * Find all courses where given account is owner or member.
     */
    public List<Group> findCoursesEstablished(final Account account) {
        @SuppressWarnings("unchecked")
        List<Group> courseAccounts = jpaApi
                .em()
                .createQuery(
                        "SELECT ga.group FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.linkType = ?2 AND ga.group.groupType = ?3  ORDER BY ga.group.title ASC")
                .setParameter(1, account.id)
                .setParameter(2, LinkType.establish)
                .setParameter(3, GroupType.course).getResultList();
        return courseAccounts;
    }

    /**
     * Find all open groups where given account is owner or member
     */
    @SuppressWarnings("unchecked")
    public List<Group> findPublicEstablished(final Account account) {
        return jpaApi
                .em()
                .createQuery(
                        "SELECT ga.group FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.linkType = ?2 AND ga.group.groupType = ?3")
                .setParameter(1, account.id)
                .setParameter(2, LinkType.establish)
                .setParameter(3, GroupType.open).getResultList();
    }

    /**
     * Find all requests and rejects for summarization under "Offene Anfragen"
     * for given Account
     *
     * @param account Account instance
     * @return List of group accounts
     */
    public List<GroupAccount> findRequests(Account account) {
        @SuppressWarnings("unchecked")
        List<GroupAccount> groupAccounts = jpaApi
                .em()
                .createQuery(
                        "SELECT ga FROM GroupAccount ga WHERE ((ga.group.owner.id = ?1 OR ga.account.id = ?1) AND ga.linkType = ?2) OR (ga.account.id = ?1 AND ga.linkType = ?3) OR (ga.account.id = ?1 AND ga.linkType = ?4)")
                .setParameter(1, account.id).setParameter(2, LinkType.request)
                .setParameter(3, LinkType.reject).setParameter(4, LinkType.invite).getResultList();
        return groupAccounts;
    }

    /**
     * Has account any link-types to given group?
     *
     * @param account Account instance
     * @param group   Group instance
     * @return True, if an account has a link type for a group
     */
    public boolean hasLinkTypes(Account account, Group group) {
        try {
            jpaApi.em().createQuery("SELECT ga FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.group.id = ?2")
                    .setParameter(1, account.id).setParameter(2, group.id).getSingleResult();
        } catch (NoResultException exp) {
            return false;
        }
        return true;
    }

    /**
     * Retrieve Accounts from Group with given LinkType.
     */
    public List<Account> findAccountsByGroup(final Group group, final LinkType type) {
        @SuppressWarnings("unchecked")
        List<Account> accounts = (List<Account>) jpaApi
                .em()
                .createQuery(
                        "SELECT ga.account FROM GroupAccount ga WHERE ga.group.id = ?1 AND ga.linkType = ?2")
                .setParameter(1, group.id).setParameter(2, type)
                .getResultList();
        return accounts;
    }

    /**
     * Retrieve Accounts from Group with given LinkType.
     */
    @SuppressWarnings("unchecked")
    public static List<Account> staticFindAccountsByGroup(final Group group, final LinkType type) {
        return JPA.createFor("defaultPersistenceUnit").withTransaction(() -> {
            return (List<Account>) JPA
                    .em()
                    .createQuery(
                            "SELECT ga.account FROM GroupAccount ga WHERE ga.group.id = ?1 AND ga.linkType = ?2")
                    .setParameter(1, group.id).setParameter(2, type)
                    .getResultList();
        });
    }

    /**
     * Returns a groupAccount by account and group.
     *
     * @param account Account instance
     * @param group   Group instance
     * @return Group account instance
     */
    public GroupAccount find(Account account, Group group) {
        try {
            return (GroupAccount) jpaApi
                    .em()
                    .createQuery(
                            "SELECT ga FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.group.id = ?2")
                    .setParameter(1, account.id).setParameter(2, group.id)
                    .getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    public List<Long> findAccountIdsByGroup(final Group group, final LinkType type) {
        @SuppressWarnings("unchecked")
        List<Long> accounts = (List<Long>) jpaApi
                .em()
                .createQuery(
                        "SELECT ga.account.id FROM GroupAccount ga WHERE ga.group.id = ?1 AND ga.linkType = ?2")
                .setParameter(1, group.id).setParameter(2, type)
                .getResultList();
        return accounts;
    }
}
