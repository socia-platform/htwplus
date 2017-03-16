package managers;

import akka.actor.ActorSystem;
import models.*;
import models.enums.GroupType;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import play.libs.F;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by Iven on 16.12.2015.
 */
public class GroupAccountManager implements BaseManager {

    @Inject
    ElasticsearchService elasticsearchService;
    @Inject
    NotificationManager notificationManager;
    @Inject
    PostManager postManager;
    @Inject
    JPAApi jpaApi;

    @Override
    public void create(Object model) {
        jpaApi.em().persist(model);
        reIndex(((GroupAccount) model).group);
    }

    @Override
    public void update(Object model) {
        jpaApi.em().merge(model);
        reIndex(((GroupAccount) model).group);
    }

    @Override
    public void delete(Object model) {
        GroupAccount groupAccount = (GroupAccount) model;
        jpaApi.em().remove(groupAccount);
        reIndex(groupAccount.group);
        notificationManager.deleteReferencesForAccountId(groupAccount.group, groupAccount.account.id);
    }

    public GroupAccount findById(Long id) {
        return jpaApi.em().find(GroupAccount.class, id);
    }

    public static GroupAccount findById2(Long id) {
        return JPA.em().find(GroupAccount.class, id);
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
    public static List<Account> findAccountsByGroup2(final Group group, final LinkType type) {
        @SuppressWarnings("unchecked")
        List<Account> accounts = (List<Account>) JPA
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
     * Retrieve AccountsId from Group with given LinkType.
     */
    public static List<Long> findAccountIdsByGroup2(final Group group, final LinkType type) {
        @SuppressWarnings("unchecked")
        List<Long> accounts = (List<Long>) JPA
                .em()
                .createQuery(
                        "SELECT ga.account.id FROM GroupAccount ga WHERE ga.group.id = ?1 AND ga.linkType = ?2")
                .setParameter(1, group.id).setParameter(2, type)
                .getResultList();
        return accounts;
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

    /**
     * each group document contains information about their member
     * if a user gets access to this group -> (re)index group document
     * and (re)index all containing post documents
     *
     * @param group group which should be indexed
     */
    private void reIndex(Group group) {
        ActorSystem system = ActorSystem.create();
        // reindexing can be very time consuming -> do it in an own thread.
        system.scheduler().scheduleOnce(
            Duration.create(3, TimeUnit.SECONDS),
            new Runnable() {
                public void run() {
                    jpaApi.withTransaction(() -> {
                        try {
                            elasticsearchService.index(group);
                            for (Post post : postManager.getPostsForGroup(group, 0, 0)) {
                                elasticsearchService.index(post);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            },
            system.dispatcher()
        );
    }

    /**
     * filter GroupAccounts by LinkType
     * @param groupAccountList list of groupAccounts
     * @param linkType to filter
     * @return Accounts
     */
    public static List<Account> filterGroupAccountsByLinkType(Set<GroupAccount> groupAccountList, LinkType linkType) {
        List<Account> accountList = new ArrayList<>();
        for (GroupAccount groupAccount : groupAccountList) {
            if (groupAccount.linkType.equals(linkType)) {
                accountList.add(groupAccount.account);
            }
        }
        return accountList;
    }
}
