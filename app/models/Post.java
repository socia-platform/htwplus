package models;

import java.io.IOException;
import java.util.*;

import javax.persistence.*;
import javax.persistence.Query;

import models.enums.AccountRole;
import models.enums.GroupType;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import models.base.BaseModel;
import models.base.BaseNotifiable;
import models.base.INotifiable;
import net.hamnaberg.json.*;
import net.hamnaberg.json.Collection;
import net.hamnaberg.json.Error;
import play.Logger;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

import org.hibernate.annotations.Type;
import util.ExposeClass;
import util.ExposeField;
import util.JsonCollectionUtil;

@Entity
@ExposeClass
public class Post extends BaseNotifiable implements INotifiable {
	public static final String GROUP = "group";                             // post to group news stream
	public static final String PROFILE = "profile";                         // post to own news stream
	public static final String STREAM = "stream";                           // post to a foreign news stream
    public static final String COMMENT_PROFILE = "comment_profile";         // comment on a profile post
    public static final String COMMENT_GROUP = "comment_group";             // comment on a group post
    public static final String COMMENT_OWN_PROFILE = "comment_profile_own"; // comment on own news stream
    public static final String BROADCAST = "broadcast";                     // broadcast post from admin control center

    @ExposeField(template = "The Content.")
    @Required
    @Lob
    @Type(type = "org.hibernate.type.TextType")
	public String content;

    @ExposeField
	@ManyToOne
	public Post parent;

    @ExposeField
	@ManyToOne
	public Group group;

    @ExposeField
	@ManyToOne
	public Account account;

    @ExposeField
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

    @Transient
    public String searchContent;

    public Post() {}

    public Post(Collection col) {
        Map<String, Property> data = col.getFirstItem().get().getDataAsMap();
        owner = Account.findById(Long.parseLong(data.get("owner_id").getValue().get().asString()));
        content = data.get("content").getValue().get().asString();
        String account_id = data.get("account_id").getValue().get().asString();
        String parent_id = data.get("parent_id").getValue().get().asString();
        String group_id = data.get("group_id").getValue().get().asString();
        if (account_id != "")
            account = Account.findById(Long.parseLong(account_id));
        else if (group_id != "")
            group = Group.findById(Long.parseLong(group_id));
        if (parent_id != "")
            parent = Post.findById(Long.parseLong(parent_id));
    }

    public static Collection validatePost(Collection col) {
        Collection validated;
        List<String> errors = new ArrayList<String>();
        if (col != null) {
            col = JsonCollectionUtil.checkForMissingItems(col, new Post());
            if (!col.hasError()) {
                Map<String, Property> data = col.getFirstItem().get().getDataAsMap();
                String account_id = data.get("account_id").getValue().get().asString();
                String owner_id = data.get("owner_id").getValue().get().asString();
                String parent_id = data.get("parent_id").getValue().get().asString();
                String group_id = data.get("group_id").getValue().get().asString();
                if (owner_id != "") {
                    if (Account.findById(Long.parseLong(owner_id)) == null)
                        errors.add("Invalid owner id: account with id " + owner_id + " does not seem to exist.");
                } else {
                    errors.add("Invalid owner id: You have to specify an owner.");
                }
                if (account_id != "") {
                    if (Account.findById(Long.parseLong(account_id)) == null)
                        errors.add("Invalid target: account with id " + account_id + " does not seem to exist.");
                }
                if (parent_id != "") {
                    if (Post.findById(Long.parseLong(parent_id)) == null)
                        errors.add("Invalid target: post with id " + parent_id + " does not seem to exist.");
                }
                if (group_id != "") {
                    if (Group.findById(Long.parseLong(group_id)) == null) {
                        errors.add("Invalid target: group with id " + group_id + " does not seem to exist.");
                    }
                }
                if (data.get("group_id").getValue().get().asString() != "" && data.get("account_id").getValue().get().asString() != "") {
                    errors.add("Invalid target: You can either post to a group or an account. Not both.");
                }
                if (data.get("group_id").getValue().get().asString() == "" && data.get("account_id").getValue().get().asString() == "") {
                    errors.add("Invalid target: You have to specify a target group_id or account_id.");
                }
                if (!errors.isEmpty()) {
                    Error error = Error.create("Invalid post data", "422", "Errors: " + String.join(" ", errors));
                    validated = Collection.create(col.getHref().get(), col.getLinks(), col.getItems(), col.getQueries(), col.getTemplate().get(), error);
                } else
                    validated = col;
            } else
                validated = col;
        } else {
            Collection withError = JsonCollectionUtil.getErrorCollection(Post.class, "Invalid Json+Collection", "422", "Non parsable Json+Collection, please check your syntax");
            validated = JsonCollectionUtil.addTemplate(Post.class, withError);
        }
        return validated;
    }

	public void create() {
		JPA.em().persist(this);
        try {
            if (!this.owner.role.equals(AccountRole.ADMIN))
            ElasticsearchService.indexPost(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String validate() {
        if(this.content.trim().length() <= 0) {
            return "Empty post!";
        }
        return null;
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

        Notification.deleteReferences(this);

        // Delete Elasticsearch document
        ElasticsearchService.deletePost(this);

        JPA.em().remove(this);
	}

	protected static Query limit(Query query, int limit, int offset) {
		query.setMaxResults(limit);
		if (offset >= 0) {
			query.setFirstResult(offset);
		}
		return query;
	}

	public static Post findById(Long id) {
        return JPA.em().find(Post.class, id);
	}

	@SuppressWarnings("unchecked")
	public static List<Post> getPostsForGroup(final Group group, final int limit, final int page) {
		Query query = JPA.em()
                .createQuery("SELECT p FROM Post p WHERE p.group.id = ?1 ORDER BY p.createdAt DESC")
                .setParameter(1, group.id);

        int offset = (page * limit) - limit;
        query = limit(query, limit, offset);

        return query.getResultList();
	}

	public static int countPostsForGroup(final Group group) {
		return ((Number)JPA.em().createQuery("SELECT COUNT(p) FROM Post p WHERE p.group.id = ?1").setParameter(1, group.id).getSingleResult()).intValue();
	}


	@SuppressWarnings("unchecked")
	public static List<Post> getCommentsForPost(Long id, int limit, int offset) {
		Query query = JPA.em()
                .createQuery("SELECT p FROM Post p WHERE p.parent.id = ?1 ORDER BY p.createdAt ASC")
                .setParameter(1, id);

		query = limit(query, limit, offset);

		return (List<Post>) query.getResultList();
	}

    @SuppressWarnings("unchecked")
	public static List<Post> findStreamForAccount(final Account account, final List<Group> groupList, final List<Account> friendList, final List<Post> bookmarkList, final String filter, final int limit, final int offset) {
        	Query query = streamForAccount("SELECT DISTINCT p ", account, groupList, friendList, bookmarkList, filter, " ORDER BY p.updatedAt DESC");

            // set limit and offset
            query = limit(query, limit, offset);
            return query.getResultList();
	}

	public static int countStreamForAccount(final Account account, final List<Group> groupList, final List<Account> friendList, final List<Post> bookmarkList, final String filter) {
		final Query query = streamForAccount("SELECT DISTINCT COUNT(p)", account, groupList, friendList, bookmarkList, filter, "");
        return ((Number) query.getSingleResult()).intValue();
	}

	/**
	 * @param account - Account (usually: current user or a contact)
	 * @param groupList - a list containing all groups we want to search in (usually all groups from account)
	 * @param accountList - a list containing all accounts we want to search in (usually contact from account)
	 * @return List of Posts
	 */
	public static Query streamForAccount(String selectClause, Account account, List<Group> groupList, List<Account> accountList, List<Post> bookmarkList, String filter, String orderByClause){

        HashMap<String, String> streamClausesMap = new HashMap<>();
        List<String> streamClausesList = new ArrayList<>();

        // find stream posts from @account
        String accountPosts = " (p.owner = (:account) AND p.account = (:account)) ";
        streamClausesMap.put("accountPosts", accountPosts);



        if(groupList != null && groupList.size() != 0) {
            // find group posts from @account
            String accountGroupPosts = " (p.owner = (:account) AND p.group IN (:groupList)) ";
            streamClausesMap.put("accountGroupPosts", accountGroupPosts);

            // find posts from each group in @groupList
            String allGroupPosts = " (p.group IN (:groupList)) ";
            streamClausesMap.put("allGroupPosts", allGroupPosts);
        }

        if(accountList != null && accountList.size() != 0) {
            // find posts from @account where @account posted on @accountList
            String accountContactPosts = " (p.owner = (:account) AND p.account IN (:accountList)) ";
            streamClausesMap.put("accountContactPosts", accountContactPosts);

            // find posts from @accountList where @accountList posted on @account's feed
            String contactToAccountPosts = " (p.owner IN (:accountList) AND p.account = (:account)) ";
            streamClausesMap.put("contactToAccountPosts", contactToAccountPosts);

            // find posts from @accountList which are posted on his/her own feed
            String contactPosts = " (p.owner IN (:accountList) AND p.account = p.owner) ";
            streamClausesMap.put("contactPosts", contactPosts);
        }

        if(bookmarkList != null && bookmarkList.size() != 0) {
            // find bookmarked posts from @account
            String bookmarkPosts = " (p IN (:bookmarkList)) ";
            streamClausesMap.put("bookmarkPosts", bookmarkPosts);

        }

        switch (filter) {
            case "group":
                streamClausesList.add(streamClausesMap.get("allGroupPosts"));
                break;
            case "account":
                streamClausesList.add(streamClausesMap.get("accountPosts"));
                streamClausesList.add(streamClausesMap.get("accountContactPosts"));
                streamClausesList.add(streamClausesMap.get("accountGroupPosts"));
                break;
            case "contact":
                streamClausesList.add(streamClausesMap.get("contactToAccountPosts"));
                streamClausesList.add(streamClausesMap.get("contactPosts"));
                break;
            case "visitor":
                streamClausesList.add(streamClausesMap.get("accountGroupPosts"));
                streamClausesList.add(streamClausesMap.get("contactToAccountPosts"));
                streamClausesList.add(streamClausesMap.get("accountPosts"));
                break;
            case "bookmark":
                streamClausesList.add(streamClausesMap.get("bookmarkPosts"));
                break;

            default:
                streamClausesList.add(streamClausesMap.get("accountPosts"));
                streamClausesList.add(streamClausesMap.get("accountGroupPosts"));
                streamClausesList.add(streamClausesMap.get("allGroupPosts"));
                streamClausesList.add(streamClausesMap.get("accountContactPosts"));
                streamClausesList.add(streamClausesMap.get("contactToAccountPosts"));
                streamClausesList.add(streamClausesMap.get("contactPosts"));
                break;
        }
        // its possible that @streamClausesList contains null values. remove them.
        streamClausesList.removeAll(Collections.singleton(null));


        // assemble query.
        // insert dummy where clause (1=2) for the unlikely event of empty @streamClausesList (e.g. new user with no groups or contact)
        String completeQuery = selectClause + " FROM Post p WHERE 1=2 " + assembleClauses(streamClausesList) + orderByClause;
		Query query = JPA.em().createQuery(completeQuery);

        // check @completeQuery for parameter which are needed.
        // () are necessary to distinguish between :account and :accountList
        if (completeQuery.contains("(:account)"))
            query.setParameter("account", account);

        if (completeQuery.contains("(:groupList)"))
            query.setParameter("groupList", groupList);

        if (completeQuery.contains("(:accountList)"))
            query.setParameter("accountList", accountList);

        if (completeQuery.contains("(:bookmarkList)"))
            query.setParameter("bookmarkList", bookmarkList);

        return query;
	}

    private static String assembleClauses(List<String> streamClausesList) {
        String assembledClauses = "";
        Iterator iterator = streamClausesList.iterator();
        while(iterator.hasNext()){
            assembledClauses += " OR " + iterator.next().toString();
        }

        return assembledClauses;
    }

	public static int countCommentsForPost(final Long id) {
		return ((Number)JPA.em().createQuery("SELECT COUNT(p.id) FROM Post p WHERE p.parent.id = ?1").setParameter(1, id).getSingleResult()).intValue();
    }

	public int getCountComments() {
		return Post.countCommentsForPost(this.id);
	}

	public boolean belongsToGroup() { return this.group != null; }

	public boolean belongsToAccount() { return this.account != null; }

    public boolean belongsToPost() { return this.parent != null; }

    public boolean isMine() { return this.account.equals(this.owner); }

	/**
	 * @param account Account (current user)
	 * @return List of Posts
	 */
	public static List<Post> getStream(Account account, int limit, int page) {
		// find friends and groups of given account
		List<Account> friendList = Friendship.findFriends(account);
		List<Group> groupList = GroupAccount.findEstablished(account);
        List<Post> bookmarkList = PostBookmark.findByAccount(account);

		int offset = (page * limit) - limit;
		return findStreamForAccount(account, groupList, friendList, bookmarkList, "all", limit, offset);
	}

    /**
     * @param account Account (current user)
     * @return List of Posts
     */
    public static List<Post> getFilteredStream(Account account, int limit, int page, String filter) {
        int offset = (page * limit) - limit;
        return findStreamForAccount(account, GroupAccount.findEstablished(account), Friendship.findFriends(account), PostBookmark.findByAccount(account), filter, limit, offset);
    }


	/**
	 * @param account Account (current user)
	 * @return Number of Posts
	 */
	public static int countStream(Account account, String filter) {
        return countStreamForAccount(account, GroupAccount.findEstablished(account), Friendship.findFriends(account), PostBookmark.findByAccount(account), filter);
    }

	/**
	 * @param contact - Account
	 * @return List of Posts
	 */
	public static List<Post> getFriendStream(Account contact, int limit, int page) {
		int offset = (page * limit) - limit;
		return findStreamForAccount(contact, GroupAccount.findPublicEstablished(contact), Friendship.findFriends(contact), PostBookmark.findByAccount(contact), "visitor", limit, offset);
	}

	/**
	 * @param contact - Account (a friends account)
	 * @return Number of Posts
	 */
	public static int countFriendStream(Account contact){
		return countStreamForAccount(contact, GroupAccount.findPublicEstablished(contact), Friendship.findFriends(contact), PostBookmark.findByAccount(contact),  "visitor");
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
            return controllers.routes.PostController.view(this.id).toString();
        }

        if (this.type.equals(Post.PROFILE)) {
            return controllers.routes.PostController.view(this.id).toString();
        }

        if (this.type.equals(Post.COMMENT_PROFILE)) {
            return controllers.routes.PostController.view(this.parent.id).toString();
        }

        if (this.type.equals(Post.COMMENT_GROUP)) {
            return controllers.routes.PostController.view(this.parent.id).toString();
        }

        if (this.type.equals(Post.COMMENT_OWN_PROFILE)) {
            return controllers.routes.PostController.view(this.parent.id).toString();
        }

        if (this.type.equals(Post.BROADCAST)) {
            return controllers.routes.PostController.view(this.id).toString();
        }

        return super.getTargetUrl();
    }

    /**
     * As the notifications should only refer to the main post, we need to return the parent, if given.
     * Otherwise this is the main post and we can return this.
     *
     * @return Post instance
     */
    @Override
    public BaseModel getReference() {
        if (this.parent != null) {
            return this.parent;
        }

        return this;
    }

    /**
     * As we want to have only one notification per post and just update if there is a new comment,
     * we need to find out, if there is a notification per post and user already given. If there is no
     * notification given for a user and post, we create a new notification instance.
     *
     * @param recipient Account recipient
     * @return Notification instance
     */
    @Override
    public Notification getNotification(Account recipient) {
        if (this.parent != null) {
            try {
                return Notification.findByReferenceIdAndRecipientId(this.parent.id, recipient.id);
            } catch (NoResultException ex) {
                Logger.error("Error while trying to fetch notification for Post ID: " + this.parent.id
                        + ", Recipient ID: " + recipient.id + ": " + ex.getMessage()
                );
            }
        }

        return new Notification();
    }

    /**
     * Get all posts except error posts (from Admin)
     * @return
     */
    public static List<Post> allWithoutAdmin() {
        return JPA.em().createQuery("FROM Post p WHERE p.owner.id != 1").getResultList();
    }

    public static long indexAllPosts() throws IOException {
        final long start = System.currentTimeMillis();
        for (Post post: allWithoutAdmin()) ElasticsearchService.indexPost(post);
        return (System.currentTimeMillis() - start) / 100;

    }

    /**
     * Collect all AccountIds, which are able to view this.post
     * @return List of AccountIds
     */
    public List<Long> findAllowedToViewAccountIds(){

        List<Long> viewableIds = new ArrayList<>();

        // everybody from post.group can see this post
        if(this.belongsToGroup()) {
            viewableIds.addAll(GroupAccount.findAccountIdsByGroup(this.group, LinkType.establish));
        }


        if(this.belongsToAccount()) {

            // every friend from post.account can see this post
            viewableIds.addAll(Friendship.findFriendsId(this.account));

            // the owner of this.account can see this post
            viewableIds.add(this.account.id);

            // everybody can see his own post
            if(this.isMine()) {
                viewableIds.add(this.owner.id);
            }
        }

        // multiple options if post is a comment
        if(this.belongsToPost()) {

            // every member from post.parent.group can see this post
            if(this.parent.belongsToGroup()) {
                viewableIds.addAll(GroupAccount.findAccountIdsByGroup(this.parent.group, LinkType.establish));
            }

            // every friend from post.parent.account can see this post
            if(this.parent.belongsToAccount()) {
                viewableIds.addAll(Friendship.findFriendsId(this.parent.account));

                // everybody can see his own comment
                if(this.parent.isMine()) {
                    viewableIds.add(this.owner.id);
                }
            }
        }
        return viewableIds;
    }

    public boolean isPublic() {

        // post in public group
        if(this.belongsToGroup()) {
            return this.group.groupType.equals(GroupType.open);
        }

        // comment in public group
        if(this.belongsToPost() && this.parent.belongsToGroup()) {
            return this.parent.group.groupType.equals(GroupType.open);
        }
        return false;
    }
}
