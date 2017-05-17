package managers;

import daos.GroupAccountDao;
import models.*;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Iven on 17.12.2015.
 */
public class FriendshipManager implements BaseManager {

    ElasticsearchService elasticsearchService;
    NotificationManager notificationManager;
    GroupAccountDao groupAccountDao;
    JPAApi jpaApi;

    @Inject
    public FriendshipManager(ElasticsearchService elasticsearchService,
            NotificationManager notificationManager,
                             GroupAccountDao groupAccountDao,
            JPAApi jpaApi) {
        this.elasticsearchService = elasticsearchService;
        this.notificationManager = notificationManager;
        this.groupAccountDao = groupAccountDao;
        this.jpaApi = jpaApi;

    }

    @Override
    public void create(Object model) {
        Friendship friendship = ((Friendship) model);
        jpaApi.em().persist(model);
        reIndex(friendship);
    }

    @Override
    public void update(Object model) {
        Friendship friendship = ((Friendship) model);
        jpaApi.em().merge(model);
        reIndex(friendship);
    }

    @Override
    public void delete(Object model) {
        Friendship friendship = ((Friendship) model);
        jpaApi.em().remove(friendship);
        notificationManager.deleteReferences(friendship);
        reIndex(friendship);
    }

    public Friendship findById(Long id) {
        return jpaApi.em().find(Friendship.class, id);
    }

    public Friendship findRequest(Account me, Account potentialFriend) {
        try{
            return (Friendship) jpaApi.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 AND fs.friend.id = ?2 AND fs.linkType = ?3")
                    .setParameter(1, me.id).setParameter(2, potentialFriend.id).setParameter(3, LinkType.request).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    public Friendship findReverseRequest(Account me, Account potentialFriend) {
        try{
            return (Friendship) jpaApi.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.friend.id = ?1 AND fs.account.id = ?2 AND fs.linkType = ?3")
                    .setParameter(1, me.id).setParameter(2, potentialFriend.id).setParameter(3, LinkType.request).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    public Friendship findFriendLink(Account account, Account target) {
        try{
            return (Friendship) jpaApi.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 and fs.friend.id = ?2 AND fs.linkType = ?3")
                    .setParameter(1, account.id).setParameter(2, target.id).setParameter(3, LinkType.establish).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    /**
     * Returns true, if two accounts have a friendly relationship.
     *
     * @param me Account instance
     * @param potentialFriend Account instance
     * @return True, if both accounts are friends
     */
    public boolean alreadyFriendly(Account me, Account potentialFriend) {
        try {
            jpaApi.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 and fs.friend.id = ?2 AND fs.linkType = ?3")
                    .setParameter(1, me.id).setParameter(2, potentialFriend.id).setParameter(3, LinkType.establish).getSingleResult();
        } catch (NoResultException exp) {
            return false;
        }
        return true;
    }

    public static boolean alreadyFriendly2(Account me, Account potentialFriend) {
        try {
            JPA.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 and fs.friend.id = ?2 AND fs.linkType = ?3")
                    .setParameter(1, me.id).setParameter(2, potentialFriend.id).setParameter(3, LinkType.establish).getSingleResult();
        } catch (NoResultException exp) {
            return false;
        }
        return true;
    }

    public boolean alreadyRejected(Account me, Account potentialFriend) {
        try {
            jpaApi.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 and fs.friend.id = ?2 AND fs.linkType = ?3")
                    .setParameter(1, me.id).setParameter(2, potentialFriend.id).setParameter(3, LinkType.reject).getSingleResult();
        } catch (NoResultException exp) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public List<Account> findFriends(final Account account){
        return (List<Account>) jpaApi.em().createQuery("SELECT fs.friend FROM Friendship fs WHERE fs.account.id = ?1 AND fs.linkType = ?2 ORDER BY fs.friend.firstname ASC")
                .setParameter(1, account.id).setParameter(2, LinkType.establish).getResultList();
    }

    /**
     * Lists all friendships of the specified account (in both directions and of all LinkTypes)
     * @param accountId the account id
     * @return the list of Friendships
     */
    @SuppressWarnings("unchecked")
    public List<Friendship> listAllFriendships(Long accountId){
        return jpaApi.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 OR fs.friend.id = ?1")
                .setParameter(1, accountId).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Long> findFriendsId(final Account account){
        return (List<Long>) jpaApi.em().createQuery("SELECT fs.friend.id FROM Friendship fs WHERE fs.account.id = ?1 AND fs.linkType = ?2")
                .setParameter(1, account.id).setParameter(2, LinkType.establish).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Friendship> findRequests(final Account account) {
        return (List<Friendship>) jpaApi.em().createQuery("SELECT fs FROM Friendship fs WHERE (fs.friend.id = ?1 OR fs.account.id = ?1) AND fs.linkType = ?2")
                .setParameter(1, account.id).setParameter(2, LinkType.request).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Friendship> findRejects(final Account account) {
        return (List<Friendship>) jpaApi.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 AND fs.linkType = ?2")
                .setParameter(1, account.id).setParameter(2, LinkType.reject).getResultList();
    }

    public List<Account> friendsToInvite(Account account, Group group) {
        List<Account> inevitableFriends = findFriends(account);

        if (inevitableFriends != null) {
            Iterator<Account> it = inevitableFriends.iterator();
            Account friend;

            while(it.hasNext()) {
                friend = it.next();

                //remove account from list if there is any type of link (requests, invite, already member)
                if (groupAccountDao.hasLinkTypes(friend, group)) {
                    it.remove();
                }
            }
        }

        return inevitableFriends;
    }

    private void reIndex(Friendship friendship) {
        // each account document contains information about their friends
        // if a user accepts or deletes a friendship -> (re)index both user documents
        try {
            elasticsearchService.indexAccount(friendship.account, findFriendsId(friendship.account));
            elasticsearchService.indexAccount(friendship.friend, findFriendsId(friendship.friend));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
