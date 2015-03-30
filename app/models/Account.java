package models;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.typesafe.config.ConfigFactory;
import models.base.BaseModel;
import models.base.IJsonNodeSerializable;
import models.enums.AccountRole;
import models.enums.EmailNotifications;

import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.Logger;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import controllers.Component;
import play.db.jpa.Transactional;
import play.libs.Json;

@Entity
public class Account extends BaseModel implements IJsonNodeSerializable {

	public String loginname;

	public String name;

	@Required
	public String firstname;

	@Required
	public String lastname;

	@Email
	@Column(unique=true)
	public String email;

	@Required
	public String password;
	
	public String avatar;

	@OneToMany(mappedBy = "account", orphanRemoval = true)
	public Set<Friendship> friends;
	
	@OneToMany(mappedBy="account", orphanRemoval = true)
	public Set<GroupAccount> groupMemberships;

	public Date lastLogin;

	public String studentId;

	@OneToOne(orphanRemoval = true)
	public Studycourse studycourse;
	public String degree;
	public Integer semester;

	public AccountRole role;

    public EmailNotifications emailNotifications;

    public Integer dailyEmailNotificationHour;

	public Boolean approved;

    /**
     * Returns an account by account ID.
     *
     * @param id Account ID
     * @return Account instance
     */
	public static Account findById(Long id) {
		return JPA.em().find(Account.class, id);
	}

    @SuppressWarnings("unchecked")
	public static List<Account> findAll(){
		return JPA.em().createQuery("SELECT a FROM Account a ORDER BY a.name").getResultList();
	}

	@Override
	public void create() {
		this.name = firstname+" "+lastname;
		JPA.em().persist(this);
        try {
            ElasticsearchService.indexAccount(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	@Override
	public void update() throws PersistenceException {
		this.name = this.firstname+" "+this.lastname;
		JPA.em().merge(this);
	}

	@Override
	public void delete() {
        Account dummy = Account.findByEmail(ConfigFactory.load().getString("htwplus.dummy.mail"));

        // Anonymize Posts //
        List<Post> owned = Post.listAllPostsOwnedBy(this.id);
        for(Post post : owned) {
            post.owner = dummy;
            post.create(); // elastic search indexing
            post.update();
        }
        List<Post> pinned = Post.listAllPostsPostedOnAccount(this.id);
        for(Post post : pinned) {
            post.account = dummy;
            post.create(); // elastic search indexing
            post.update();
        }

        // Anonymize created groups //
        List<Group> groups = Group.listAllGroupsOwnedBy(this.id);
        for(Group group : groups) {
            if(GroupAccount.findAccountsByGroup(group, LinkType.establish).size() == 1) { // if the owner is the only member of the group
                Logger.info("Group '" + group.title + "' is now empty, so it will be deleted!");
                group.delete();
            } else {
                group.owner = dummy;
                group.update();
            }
        }

        // Delete Friendships //
        List<Friendship> friendships = Friendship.listAllFriendships(this.id);
        for(Friendship friendship : friendships) {
            friendship.delete();
        }

        // Anonymize media //
        List<Media> media = Media.listAllOwnedBy(this.id);
        for(Media med : media) {
            med.owner = dummy;
            med.update();
        }

        // Delete incoming notifications //
        Notification.deleteNotificationsForAccount(this.id);

        // (internally) anonymize outgoing notifications //
        // The renderedContent still contains the name!  //
        // TODO: notification anonymization              //
        List<Notification> notifications = Notification.findBySenderId(this.id);
        for(Notification not : notifications) {
            not.sender = dummy;
            not.update();
        }

        ElasticsearchService.deleteAccount(this);

        JPA.em().remove(this);
    }
		
	/**
     * Retrieve a User from email.
     */
    public static Account findByEmail(String email) {
    	if(email.isEmpty()) {
    		return null;
    	}
    	try{
	    	return (Account) JPA.em()
					.createQuery("from Account a where a.email = :email")
					.setParameter("email", email).getSingleResult();
	    } catch (NoResultException exp) {
	    	return null;
		}
    }
    
	/**
     * Retrieve a User by loginname
     */
    public static Account findByLoginName(String loginName) {
    	try{
	    	return (Account) JPA.em()
					.createQuery("from Account a where a.loginname = :loginname")
					.setParameter("loginname", loginName).getSingleResult();
	    } catch (NoResultException exp) {
	    	return null;
		}
    }

    /**
     * Retrieve a User by loginname
     */
    public static Account findByName(String name) {
        try{
            return (Account) JPA.em()
                    .createQuery("from Account a where a.name = :name")
                    .setParameter("name", name).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    /**
     * Authenticates a user by email and password.
     * @param email of the user who wants to be authenticate
     * @param password of the user should match to the email ;) 
     * @return Returns the current account or Null
     */
	public static Account authenticate(String email, String password) {
		Account currentAcc = null;
		try {
			final Account result = (Account) JPA.em()
				.createQuery("from Account a where a.email = :email")
				.setParameter("email", email).getSingleResult();
			if (result != null && Component.md5(password).equals(result.password)) {
				currentAcc = result;
			}
			return currentAcc;
		} catch (NoResultException exp) {
			return currentAcc;
		}
	}
	
	public String getAvatarUrl() {
		String url = controllers.routes.Assets.at("images/avatars/" + this.avatar + ".png").toString();
		return url;
	}
	
	public static boolean isOwner(Long accountId, Account currentUser) {
		Account a = JPA.em().find(Account.class, accountId);
		if(a.equals(currentUser)){
			return true;
		} else { 
			return false;
		}
	}

	/**
	 * Try to get all accounts...
	 * @return List of accounts.
	 */
	@SuppressWarnings("unchecked")
	public static List<Account> all() {
        return JPA.em().createQuery("FROM Account").getResultList();
	}


    /**
     * Returns a list of account instances by an ID collection of Strings.
     *
     * @param accountIds String array of account IDs
     * @return List of accounts
     */
    public static List<Account> getAccountListByIdCollection(final List<String> accountIds) {
    	StringBuilder joinedAccountIds = new StringBuilder();
        for (int i = 0; i < accountIds.size(); i++) {
            if (i > 0) {
                joinedAccountIds.append(",");
            }
            joinedAccountIds.append(accountIds.get(i));
        }

        return JPA.em()
                .createQuery("FROM Account a WHERE a.id IN (" +joinedAccountIds.toString() + ")", Account.class)
                .getResultList();
    }

    @Override
    public ObjectNode getAsJson() {
        ObjectNode node = Json.newObject();
        node.put("id", this.id);
        node.put("name", this.name);

        return node;
    }

    public static List<Account> getAllNames(){
        return JPA.em().createQuery("SELECT a.id, a.name FROM Account a").getResultList();
    }

    public static long indexAllAccounts() throws IOException {
        final long start = System.currentTimeMillis();
        for (Account account: all()) ElasticsearchService.indexAccount(account);
        return (System.currentTimeMillis() - start) / 100;

    }
}