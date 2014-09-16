package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import models.base.BaseNotifiable;
import models.base.INotifiable;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import play.libs.F;

@Entity
public class Post extends BaseNotifiable implements INotifiable {
	public static final String GROUP = "group";                             // post to group news stream
	public static final String PROFILE = "profile";                         // post to own news stream
	public static final String STREAM = "stream";                           // post to a foreign news stream
    public static final String COMMENT_PROFILE = "comment_profile";         // comment on a profile post
    public static final String COMMENT_GROUP = "comment_group";             // comment on a group post
    public static final String COMMENT_OWN_PROFILE = "comment_profile_own"; // comment on own news stream
    public static final String BROADCAST = "broadcast";                     // broadcast post from admin control center
    public static final int PAGE = 1;

    @Required
	@Column(length=2000)
	public String content;

	@ManyToOne
	public Post parent;

	@ManyToOne
	public Group group;

	@ManyToOne
	public Account account;

	@ManyToOne
	public Account owner;

    @Column(name = "is_broadcast", nullable = false, columnDefinition = "boolean default false")
    public boolean isBroadcastMessage;

    @ManyToMany
    @JoinTable(
            name = "broadcast_account",
            joinColumns = { @JoinColumn(name = "post_id", referencedColumnName = "id") },
            inverseJoinColumns = { @JoinColumn(name = "account_id", referencedColumnName = "id") }
    )
    public List<Account> broadcastPostRecipients;
		
	public void create() {
		JPA.em().persist(this);
	}

	@Override
	public void update() {
		updatedAt();
	}

	@Override
	public void delete() {
		// delete all comments first
		List<Post> comments = getCommentsForPost(this.id, 0, 0);
		
		for (Post comment : comments) {
			comment.delete();
		}

        NewNotification.deleteReferences(this);
		JPA.em().remove(this);
	}
	
	protected static Query limit(Query query, int limit, int offset) {
		if (limit > 0) {
			query.setMaxResults(limit);
		}
		if (offset >= 0) {
			query.setFirstResult(offset);
		}
		return query;
	}
		
	public static Post findById(Long id) {
		return JPA.em().find(Post.class, id);
	}

	@SuppressWarnings("unchecked")
	public static List<Post> getPostsForGroup(Group group, int limit, int page) {
		Query query = JPA.em()
				.createQuery("SELECT p FROM Post p WHERE p.group.id = ?1 ORDER BY p.createdAt DESC")
				.setParameter(1, group.id);
		
		int offset = (page * limit) - limit;
		query = limit(query, limit, offset);

		return query.getResultList();
	}
	
	public static int countPostsForGroup(Group group) {
		return ((Number)JPA.em().createQuery("SELECT COUNT(p) FROM Post p WHERE p.group.id = ?1").setParameter(1, group.id).getSingleResult()).intValue();
	}
	
	
	@SuppressWarnings("unchecked")
	public static List<Post> getCommentsForPost(Long id, int start, int max) {	
		return (List<Post>) JPA.em()
				.createQuery("SELECT p FROM Post p WHERE p.parent.id = ?1 ORDER BY p.createdAt ASC")
				.setParameter(1, id)
				.setFirstResult(start)
				.setMaxResults(max)
				.getResultList();
	}

    /**
     * Method getCommentsForPostTransactional() JPA transactional.
     *
     * @param id ID of parent post
     * @param start Comment start
     * @param max Max comments
     * @return List of Posts
     */
    public static List<Post> getCommentsForPostTransactional(final Long id, final int start, final int max) {
        try {
            return JPA.withTransaction(new F.Function0<List<Post>>() {
                @Override
                public List<Post> apply() throws Throwable {
                    return Post.getCommentsForPost(id, start, max);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

        @SuppressWarnings("unchecked")
	public static List<Post> findStreamForAccount(Account account, List<Group> groupList, List<Account> friendList, boolean isVisitor, int limit, int offset){
		
		Query query = streamForAccount("SELECT DISTINCT p ", account, groupList, friendList, isVisitor, " ORDER BY p.updatedAt DESC");

		// set limit and offset
		query = limit(query, limit, offset);
        final Query finalQuery = query;

        try {
            return JPA.withTransaction(new F.Function0<List>() {
                @Override
                public List apply() throws Throwable {
                    return finalQuery.getResultList();
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
	}
	
	public static int countStreamForAccount(final Account account, List<Group> groupList, List<Account> friendList, boolean isVisitor) {
		final Query query = streamForAccount("SELECT DISTINCT COUNT(p)", account, groupList, friendList, isVisitor,"");

        try {
            return JPA.withTransaction(new F.Function0<Integer>() {
                @Override
                public Integer apply() throws Throwable {
                    return ((Number) query.getSingleResult()).intValue();
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return 0;
        }
	}
	
	/**
	 * @param account - Account (current user, profile or a friend)
	 * @param groupList - a list containing all groups we want to search in
	 * @param friendList - a list containing all friends we want to search in
	 * @return List of Posts
	 */
	public static Query streamForAccount(String selectClause, Account account, List<Group> groupList, List<Account> friendList, boolean isVisitor, String orderByClause){

		// since JPA is unable to handle empty lists (eg. groupList, friendList) we need to assemble our query.
		String myPostsClause;
		String groupListClause = "";
		String friendListClause = "";
		String visitorClause = "";
        String broadcastJoin = "";
        String broadcastClause = "";

		/**
		 *  finds all stream-post for account.
		 *  e.g account = myself
		 *  1. if i'm mentioned in post.account, somebody posted me. (yep, we want this => 'p.account = :account')
		 *  2. if i'm post.owner, i posted somewhere (yep, we want this too => 'p.owner = :account')
		 *  BUT, if i'm the owner and post.parent is set, it's only a comment. so => 'p.parent = NULL'
		 */
		myPostsClause = " p.account = :account OR (p.owner = :account AND p.parent = NULL) ";

		// add additional clauses if not null or empty

		if (friendList != null && !friendList.isEmpty()) {
			/**
			 *  finds all own stream-posts of my friends.
			 *  e.g account = a friend of mine
			 *  1. if my friend is mentioned in p.account, somebody posted him => 'p.account IN :friendList'
			 *  BUT, we only want his/her own posts. so he has to be the owner as well => 'p.account = p.owner'
			 */
			friendListClause = " OR p.account IN :friendList AND p.account = p.owner";
		}

		if (groupList != null && !groupList.isEmpty()) {
			// finds all stream-post of groups
			groupListClause = " OR p.group IN :groupList ";
		}

		if (isVisitor) {
			/**
			 * since 'myPostsClause' includes posts where the given account posted to someone ('OR (p.owner = :account AND p.parent = NULL)').
			 * we have to modify it for the friends-stream (cut it out).
			 */
			myPostsClause = " p.account = :account ";

			/**
			 * groupListClause includes all posts where my friend is member/owner of.
			 * but we only need those posts where he/she is owner of.
			 */
			if (groupList != null && !groupList.isEmpty()) {
				visitorClause = " AND p.owner = :account ";
			}
		} else {
            // its the origin user, show also broadcast messages
            broadcastJoin = " LEFT JOIN p.broadcastPostRecipients bc_recipients";
            broadcastClause = " OR bc_recipients = :account ";
        }

		// create Query.
        String completeQuery = selectClause + " FROM Post p" + broadcastJoin + " WHERE " + myPostsClause
                + groupListClause + friendListClause + visitorClause + broadcastClause + orderByClause;
		Query query = JPA.em("default").createQuery(completeQuery);
		query.setParameter("account", account);


		// add parameter as needed
		if (groupList != null && !groupList.isEmpty()) {
			query.setParameter("groupList", groupList);
		}
		if (friendList != null && !friendList.isEmpty()) {
			query.setParameter("friendList", friendList);
		}

		return query;
	}
	
	public static int countCommentsForPost(Long id) {
		return ((Number)JPA.em("default").createQuery("SELECT COUNT(p.id) FROM Post p WHERE p.parent.id = ?1").setParameter(1, id).getSingleResult()).intValue();
	}
	
	public int getCountComments() {
		return Post.countCommentsForPost(this.id);
	}
	
	public boolean belongsToGroup() {
		return this.group != null;
	}
		
	public boolean belongsToAccount() {
		return this.account != null;
	}

	/**
	 * @param account Account (current user)
	 * @return List of Posts
	 */
	public static List<Post> getStream(Account account, int limit, int page) {
		// find friends and groups of given account
		List<Account> friendList = Friendship.findFriends(account);
		List<Group> groupList = GroupAccount.findEstablishedTransactional(account);
		
		int offset = (page * limit) - limit;
		return findStreamForAccount(account, groupList, friendList, false, limit, offset);
	}
	
	
	/**
	 * @param account Account (current user)
	 * @return Number of Posts
	 */
	public static int countStream(Account account){
		// find friends and groups of given account
		List<Account> friendList = Friendship.findFriends(account);
		List<Group> groupList = GroupAccount.findEstablishedTransactional(account);
			
		return countStreamForAccount(account, groupList, friendList, false);
	}
	
	/**
	 * @param friend - Account (a friends account)
	 * @return List of Posts
	 */
	public static List<Post> getFriendStream(Account friend, int limit, int page) {
		// find open groups for given account
		List<Group> groupList = GroupAccount.findPublicEstablished(friend);
			
		int offset = (page * limit) - limit;
		return findStreamForAccount(friend, groupList, null, true, limit, offset);
	}
	
	/**
	 * @param friend - Account (a friends account)
	 * @return Number of Posts
	 */
	public static int countFriendStream(Account friend){
		// find groups of given account
		List<Group> groupList = GroupAccount.findPublicEstablished(friend);
		
		return countStreamForAccount(friend, groupList, null, true);
	}

    @Override
    public Account getSender() {
        return this.owner;
    }

    @Override
    public List<Account> getRecipients() {
        if (this.type.equals(Post.BROADCAST)) {
            return this.broadcastPostRecipients;
        }

        // if this is a comment, return the parent information
        if (this.parent != null) {
            // if this is a comment on own news stream, send notification to the initial poster
            if (this.type.equals(Post.COMMENT_OWN_PROFILE)) {
                return this.parent.getAsAccountList(this.parent.owner);
            }

            return this.parent.belongsToAccount()
                    ? this.parent.getAsAccountList(this.parent.account)
                    : this.parent.getGroupAsAccountList(this.parent.group);
        }

        // return account if not null, otherwise group
        return this.belongsToAccount()
                ? this.getAsAccountList(this.account)
                : this.getGroupAsAccountList(this.group);
    }

    /**
     * Adds an account to the persistent recipient list.
     *
     * @param recipient One of the recipients
     */
    public void addRecipient(Account recipient) {
        if (this.broadcastPostRecipients == null) {
            this.broadcastPostRecipients = new ArrayList<>();
        }

        if (!this.broadcastPostRecipients.contains(recipient)) {
            this.broadcastPostRecipients.add(recipient);
        }
    }

    @Override
    public String getTargetUrl() {
        if (this.type.equals(Post.GROUP)) {
            return controllers.routes.GroupController.view(this.group.id, Post.PAGE).toString();
        }

        if (this.type.equals(Post.PROFILE)) {
            return controllers.routes.ProfileController.stream(this.account.id, Post.PAGE).toString();
        }

        if (this.type.equals(Post.COMMENT_PROFILE)) {
            return controllers.routes.ProfileController.stream(this.parent.account.id, Post.PAGE).toString();
        }

        if (this.type.equals(Post.COMMENT_GROUP)) {
            return controllers.routes.GroupController.view(this.parent.group.id, Post.PAGE).toString();
        }

        if (this.type.equals(Post.COMMENT_OWN_PROFILE)) {
            return controllers.routes.ProfileController.stream(this.parent.account.id, Post.PAGE).toString();
        }

        if (this.type.equals(Post.BROADCAST)) {
            return controllers.routes.Application.stream(Post.PAGE).toString();
        }

        return super.getTargetUrl();
    }
}