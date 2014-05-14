package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Query;

import models.base.BaseModel;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

@Entity
public class Post extends BaseModel {
	
	public static String GROUP = "group";
	public static String PROFILE = "profile";
	public static String STREAM = "stream";

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
		
	public void create() {
		JPA.em().persist(this);
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete() {
		// delete all comments first
		List<Post> comments = getCommentsForPost(this.id, 0, 0);
		
		for(Post comment : comments){
			comment.delete();
		}
		Notification.deleteByObject(this.id);
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
		
		List<Post> posts = query.getResultList();
		return posts;
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
	
	
	@SuppressWarnings("unchecked")
	public static List<Post> findStreamForAccount(Account account, List<Group> groupList, List<Account> friendList, boolean isVisitor, int limit, int offset){
		
		Query query = streamForAccount("SELECT p ", account, groupList, friendList, isVisitor, " ORDER BY p.id DESC");

		// set limit and offset
		query = limit(query, limit, offset);
		List<Post> posts = query.getResultList();
		return posts;
	}
	
	public static int countStreamForAccount(Account account, List<Group> groupList, List<Account> friendList, boolean isVisitor){
		
		Query query = streamForAccount("SELECT COUNT(p)", account, groupList, friendList, isVisitor,"");
		
		int count = ((Number) query.getSingleResult()).intValue();
		
		return count;
	}
	
	/**
	 * @author Iven
	 * @param account - Account (current user, profile or a friend)
	 * @param groupList - a list containing all groups we want to search in
	 * @param friendList - a list containing all friends we want to search in
	 * @return List of Posts
	 */
	public static Query streamForAccount(String selectClause, Account account, List<Group> groupList, List<Account> friendList, boolean isVisitor, String orderByClause){
		
		// since JPA is unable to handle empty lists (eg. groupList, friendList) we need to assemble our query.
		String myPostsClause = "";
		String groupListClause = "";
		String friendListClause = "";
		String visitorClause = "";
		
		/**
		 *  finds all stream-post for account. 
		 *  e.g account = myself
		 *  1. if i'm mentioned in post.account, somebody posted me. (yep, we want this => 'p.account = :account') 
		 *  2. if i'm post.owner, i posted somewhere (yep, we want this too => 'p.owner = :account')
		 *  BUT, if i'm the owner and post.parent is set, it's only a comment. so => 'p.parent = NULL'
		 */
		myPostsClause = " p.account = :account OR (p.owner = :account AND p.parent = NULL) ";
		
		// add additional clauses if not null or empty
		
		if(friendList != null && !friendList.isEmpty()){
			/**
			 *  finds all own stream-posts of my friends.
			 *  e.g account = a friend of mine
			 *  1. if my friend is mentioned in p.account, somebody posted him => 'p.account IN :friendList'
			 *  BUT, we only want his/her own posts. so he has to be the owner as well => 'p.account = p.owner'
			 */
			friendListClause = " OR p.account IN :friendList AND p.account = p.owner";
		}
		
		if(groupList != null && !groupList.isEmpty()){
			// finds all stream-post of groups
			groupListClause = " OR p.group IN :groupList ";
		}
		
		if(isVisitor){
			/**
			 * since 'myPostsClause' includes posts where the given account posted to someone ('OR (p.owner = :account AND p.parent = NULL)').
			 * we have to modify it for the friends-stream (cut it out).
			 */
			myPostsClause = " p.account = :account ";
			
			/**
			 * groupListClause includes all posts where my friend is member/owner of.
			 * but we only need those posts where he/she is owner of.
			 */
			if(groupList != null && !groupList.isEmpty()){
				visitorClause = " AND p.owner = :account ";
			}
			
		}
		
		// create Query. 
		Query query = JPA.em().createQuery(selectClause+" FROM Post p WHERE "+myPostsClause+groupListClause+friendListClause+visitorClause+orderByClause);
		query.setParameter("account", account);
		
		
		// add parameter as needed
		if(groupList != null && !groupList.isEmpty()){
			query.setParameter("groupList", groupList);
		}
		if(friendList != null && !friendList.isEmpty()){
			query.setParameter("friendList", friendList);
		}
		
		return query;
		
	}
	
	public static int countCommentsForPost(Long id) {
		return ((Number)JPA.em().createQuery("SELECT COUNT(p.id) FROM Post p WHERE p.parent.id = ?1").setParameter(1, id).getSingleResult()).intValue();
	}
	
	public int getCountComments() {
		return Post.countCommentsForPost(this.id);
	}
	
	public boolean belongsToGroup(){
		if(this.group != null) return true;
		return false;
	}
		
	public boolean belongsToAccount(){
		if(this.account != null) return true;
		return false;
	}

	/**
	 * @author Iven
	 * @param currentUser - Account (current user)
	 * @return List of Posts
	 */
	public static List<Post> getStream(Account account, int limit, int page) {
		// find friends and groups of given account
		List<Account> friendList = Friendship.findFriends(account);
		List<Group> groupList = GroupAccount.findEstablished(account);
		
		int offset = (page * limit) - limit;
		return findStreamForAccount(account, groupList, friendList, false, limit, offset);
	}
	
	
	/**
	 * @author Iven
	 * @param currentUser - Account (current user)
	 * @return 
	 * @return Number of Posts
	 */
	public static int countStream(Account account){
		// find friends and groups of given account
		List<Account> friendList = Friendship.findFriends(account);
		List<Group> groupList = GroupAccount.findEstablished(account);
			
		return countStreamForAccount(account, groupList, friendList, false);
	}
	
	/**
	 * @author Iven
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
	 * @author Iven
	 * @param friend - Account (a friends account)
	 * @return 
	 * @return Number of Posts
	 */
	public static int countFriendStream(Account friend){
		// find groups of given account
		List<Group> groupList = GroupAccount.findPublicEstablished(friend);
		
		return countStreamForAccount(friend, groupList, null, true);
	}
	
	
}