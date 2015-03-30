package models;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import models.base.BaseNotifiable;
import models.base.INotifiable;
import models.services.ElasticsearchService;
import play.db.jpa.JPA;

import models.enums.LinkType;
import play.libs.F;

@Entity
@Table(uniqueConstraints=
@UniqueConstraint(columnNames = {"account_id", "friend_id"}))
public class Friendship extends BaseNotifiable implements INotifiable {
    public static final String FRIEND_REQUEST_SUCCESS = "request_successful";
    public static final String FRIEND_REQUEST_DECLINE = "request_decline";
    public static final String FRIEND_NEW_REQUEST = "new_request";
    public static final int PAGE = 1;
	
	@ManyToOne
	@NotNull
	public Account account;
	
	@ManyToOne
	@NotNull
	public Account friend;

	@Enumerated(EnumType.STRING)
	@NotNull
	public LinkType linkType;
	
	public Friendship() {
	}
	
	public Friendship(Account account, Account friend, LinkType type) {
		this.account = account;
		this.friend = friend;
		this.linkType = type;
	}
	
	public static Friendship findById(Long id) {
		return JPA.em().find(Friendship.class, id);
	}

	@Override
	public void create() {
		JPA.em().persist(this);
	}

	@Override
	public void update() {
		JPA.em().merge(this);

        // each account document contains information about their friends
        // if a user accepts a friendship -> (re)index this.account document
        try {
            ElasticsearchService.indexAccount(this.account);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	@Override
	public void delete() {
        JPA.em().remove(this);

        Notification.deleteReferences(this);

        // each account document contains information about their friends
        // if a user deletes his friendship -> (re)index this.account document
        try {
            ElasticsearchService.indexAccount(this.account);
        } catch (IOException e) {
            e.printStackTrace();
        }

	}

	public static Friendship findRequest(Account me, Account potentialFriend) {
		try{
			return (Friendship) JPA.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 AND fs.friend.id = ?2 AND fs.linkType = ?3")
			.setParameter(1, me.id).setParameter(2, potentialFriend.id).setParameter(3, LinkType.request).getSingleResult();
		} catch (NoResultException exp) {
			return null;
		}
	}
	
	public static Friendship findReverseRequest(Account me, Account potentialFriend) {
		try{
			return (Friendship) JPA.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.friend.id = ?1 AND fs.account.id = ?2 AND fs.linkType = ?3")
			.setParameter(1, me.id).setParameter(2, potentialFriend.id).setParameter(3, LinkType.request).getSingleResult();
		} catch (NoResultException exp) {
			return null;
		}
	}
	
	public static Friendship findFriendLink(Account account, Account target) {
		try{
			return (Friendship) JPA.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 and fs.friend.id = ?2 AND fs.linkType = ?3")
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
	public static boolean alreadyFriendly(Account me, Account potentialFriend) {
		try {
			JPA.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 and fs.friend.id = ?2 AND fs.linkType = ?3")
			.setParameter(1, me.id).setParameter(2, potentialFriend.id).setParameter(3, LinkType.establish).getSingleResult();
		} catch (NoResultException exp) {
	    	return false;
		}
		return true;
	}
	
	public static boolean alreadyRejected(Account me, Account potentialFriend) {
		try {
			JPA.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 and fs.friend.id = ?2 AND fs.linkType = ?3")
			.setParameter(1, me.id).setParameter(2, potentialFriend.id).setParameter(3, LinkType.reject).getSingleResult();
		} catch (NoResultException exp) {
	    	return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public static List<Account> findFriends(final Account account){
        return (List<Account>) JPA.em().createQuery("SELECT fs.friend FROM Friendship fs WHERE fs.account.id = ?1 AND fs.linkType = ?2 ORDER BY fs.friend.firstname ASC")
                .setParameter(1, account.id).setParameter(2, LinkType.establish).getResultList();
    }

    /**
     * Lists all friendships of the specified account (in both directions and of all LinkTypes)
     * @param accountId the account id
     * @return the list of Friendships
     */
    @SuppressWarnings("unchecked")
    public static List<Friendship> listAllFriendships(Long accountId){
        return JPA.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 OR fs.friend.id = ?1")
                .setParameter(1, accountId).getResultList();
    }

    public static List<Long> findFriendsId(final Account account){
        return (List<Long>) JPA.em().createQuery("SELECT fs.friend.id FROM Friendship fs WHERE fs.account.id = ?1 AND fs.linkType = ?2")
                .setParameter(1, account.id).setParameter(2, LinkType.establish).getResultList();
    }
	
	@SuppressWarnings("unchecked")
	public static List<Friendship> findRequests(final Account account) {
		return (List<Friendship>) JPA.em().createQuery("SELECT fs FROM Friendship fs WHERE (fs.friend.id = ?1 OR fs.account.id = ?1) AND fs.linkType = ?2")
                .setParameter(1, account.id).setParameter(2, LinkType.request).getResultList();
	}
	
	@SuppressWarnings("unchecked")
	public static List<Friendship> findRejects(final Account account) {
		return (List<Friendship>) JPA.em().createQuery("SELECT fs FROM Friendship fs WHERE fs.account.id = ?1 AND fs.linkType = ?2")
                .setParameter(1, account.id).setParameter(2, LinkType.reject).getResultList();
	}
	
	public static List<Account> friendsToInvite(Account account, Group group) {
		List<Account> inevitableFriends = findFriends(account);
		
		if (inevitableFriends != null) {
			Iterator<Account> it = inevitableFriends.iterator();
			Account friend;
			
			while(it.hasNext()) {
				friend = it.next();
				
				//remove account from list if there is any type of link (requests, invite, already member)
				if (GroupAccount.hasLinkTypes(friend, group)) {
					it.remove();
				}
			}	        
		}
		
		return inevitableFriends;
	}

    @Override
    public Account getSender() {
        return this.type.equals(Friendship.FRIEND_REQUEST_DECLINE)
                ? this.friend
                : this.account;
    }

    @Override
    public List<Account> getRecipients() {
        return this.type.equals(Friendship.FRIEND_REQUEST_DECLINE)
                ? this.getAsAccountList(this.account)
                : this.getAsAccountList(this.friend);
    }

    @Override
    public String getTargetUrl() {
        if (this.type.equals(Friendship.FRIEND_NEW_REQUEST) || this.type.equals(Friendship.FRIEND_REQUEST_DECLINE)) {
            return controllers.routes.FriendshipController.index().toString();
        }

        if (this.type.equals(Friendship.FRIEND_REQUEST_SUCCESS)) {
            return controllers.routes.ProfileController.stream(this.account.id, Friendship.PAGE).toString();
        }

        return super.getTargetUrl();
    }
}
