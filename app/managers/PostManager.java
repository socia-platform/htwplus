package managers;

import models.Account;
import models.Group;
import models.Post;
import models.enums.GroupType;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.Configuration;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.persistence.Query;
import java.io.IOException;
import java.util.*;

/**
 * Created by Iven on 17.12.2015.
 */
public class PostManager implements BaseManager {

    @Inject
    ElasticsearchService elasticsearchService;
    @Inject
    NotificationManager notificationManager;
    @Inject
    FriendshipManager friendshipManager;
    @Inject
    GroupAccountManager groupAccountManager;
    @Inject
    PostBookmarkManager postBookmarkManager;
    @Inject
    GroupManager groupManager;
    @Inject
    Configuration configuration;
    @Inject
    JPAApi jpaApi;


    @Override
    public void create(Object model) {
        Post post = (Post) model;

        jpaApi.em().persist(post);
        try {
            elasticsearchService.index(post);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createWithoutIndex(Post post) {
        jpaApi.em().persist(post);
    }

    @Override
    public void update(Object model) {
        ((Post) model).updatedAt();
    }

    @Override
    public void delete(Object model) {
        Post post = (Post) model;

        // delete all comments first
        List<Post> comments = getCommentsForPost(post.id, 0, 0);

        for (Post comment : comments) {
            delete(comment);
        }

        notificationManager.deleteReferences(post);

        jpaApi.em().remove(post);

        // Delete Elasticsearch document
        elasticsearchService.delete(post);
    }

    @SuppressWarnings("unchecked")
    public List<Post> getCommentsForPost(Long id, int limit, int offset) {
        Query query = jpaApi.em()
                .createQuery("SELECT p FROM Post p WHERE p.parent.id = ?1 ORDER BY p.createdAt ASC")
                .setParameter(1, id);

        query = limit(query, limit, offset);

        return (List<Post>) query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public static List<Post> getCommentsForPost2(Long id, int limit, int offset) {
        Query query = JPA.em()
                .createQuery("SELECT p FROM Post p WHERE p.parent.id = ?1 ORDER BY p.createdAt ASC")
                .setParameter(1, id);

        query = limit(query, limit, offset);

        return (List<Post>) query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Post> getPostsForGroup(final Group group, final int limit, final int page) {
        Query query = jpaApi.em()
                .createQuery("SELECT p FROM Post p WHERE p.group.id = ?1 ORDER BY p.updatedAt DESC")
                .setParameter(1, group.id);

        int offset = (page * limit) - limit;
        query = limit(query, limit, offset);

        return query.getResultList();
    }

    private static Query limit(Query query, int limit, int offset) {
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        if (offset >= 0) {
            query.setFirstResult(offset);
        }
        return query;
    }

    public Post findById(Long id) {
        return jpaApi.em().find(Post.class, id);
    }

    public int countPostsForGroup(final Group group) {
        return ((Number) jpaApi.em().createQuery("SELECT COUNT(p) FROM Post p WHERE p.group.id = ?1").setParameter(1, group.id).getSingleResult()).intValue();
    }

    @SuppressWarnings("unchecked")
    public List<Post> findStreamForAccount(final Account account, final List<Group> groupList, final List<Account> friendList, final List<Post> bookmarkList, final String filter, final int limit, final int offset) {
        Query query = streamForAccount("SELECT DISTINCT p ", account, groupList, friendList, bookmarkList, filter, " ORDER BY p.updatedAt DESC");

        // set limit and offset
        query = limit(query, limit, offset);
        return query.getResultList();
    }

    public int countStreamForAccount(final Account account, final List<Group> groupList, final List<Account> friendList, final List<Post> bookmarkList, final String filter) {
        final Query query = streamForAccount("SELECT DISTINCT COUNT(p)", account, groupList, friendList, bookmarkList, filter, "");
        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * @param account     - Account (usually: current user or a contact)
     * @param groupList   - a list containing all groups we want to search in (usually all groups from account)
     * @param accountList - a list containing all accounts we want to search in (usually contact from account)
     * @return List of Posts
     */
    public Query streamForAccount(String selectClause, Account account, List<Group> groupList, List<Account> accountList, List<Post> bookmarkList, String filter, String orderByClause) {

        HashMap<String, String> streamClausesMap = new HashMap<>();
        List<String> streamClausesList = new ArrayList<>();

        // find stream posts from @account
        String accountPosts = " (p.owner = (:account) AND p.account = (:account)) ";
        streamClausesMap.put("accountPosts", accountPosts);


        if (groupList != null && groupList.size() != 0) {
            // find group posts from @account
            String accountGroupPosts = " (p.owner = (:account) AND p.group IN (:groupList)) ";
            streamClausesMap.put("accountGroupPosts", accountGroupPosts);

            // find posts from each group in @groupList
            String allGroupPosts = " (p.group IN (:groupList)) ";
            streamClausesMap.put("allGroupPosts", allGroupPosts);
        }

        if (accountList != null && accountList.size() != 0) {
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

        if (bookmarkList != null && bookmarkList.size() != 0) {
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
        Query query = jpaApi.em().createQuery(completeQuery);

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
        while (iterator.hasNext()) {
            assembledClauses += " OR " + iterator.next().toString();
        }

        return assembledClauses;
    }

    public int countCommentsForPost(final Long id) {
        return ((Number) jpaApi.em().createQuery("SELECT COUNT(p.id) FROM Post p WHERE p.parent.id = ?1").setParameter(1, id).getSingleResult()).intValue();
    }

    public static int countCommentsForPost2(final Long id) {
        return ((Number) JPA.em().createQuery("SELECT COUNT(p.id) FROM Post p WHERE p.parent.id = ?1").setParameter(1, id).getSingleResult()).intValue();
    }

    /**
     * @param account Account (current user)
     * @return List of Posts
     */
    public List<Post> getStream(Account account, int limit, int page) {
        // find friends and groups of given account
        List<Account> friendList = friendshipManager.findFriends(account);
        List<Group> groupList = groupAccountManager.findEstablished(account);
        List<Post> bookmarkList = postBookmarkManager.findByAccount(account);

        int offset = (page * limit) - limit;
        return findStreamForAccount(account, groupList, friendList, bookmarkList, "all", limit, offset);
    }

    /**
     * @param account Account (current user)
     * @return List of Posts
     */
    public List<Post> getFilteredStream(Account account, int limit, int page, String filter) {
        int offset = (page * limit) - limit;
        return findStreamForAccount(account, groupAccountManager.findEstablished(account), friendshipManager.findFriends(account), postBookmarkManager.findByAccount(account), filter, limit, offset);
    }


    /**
     * @param account Account (current user)
     * @return Number of Posts
     */
    public int countStream(Account account, String filter) {
        return countStreamForAccount(account, groupAccountManager.findEstablished(account), friendshipManager.findFriends(account), postBookmarkManager.findByAccount(account), filter);
    }

    /**
     * @param contact - Account
     * @return List of Posts
     */
    public List<Post> getFriendStream(Account contact, int limit, int page) {
        int offset = (page * limit) - limit;
        return findStreamForAccount(contact, groupAccountManager.findPublicEstablished(contact), friendshipManager.findFriends(contact), postBookmarkManager.findByAccount(contact), "visitor", limit, offset);
    }

    /**
     * @param contact - Account (a friends account)
     * @return Number of Posts
     */
    public int countFriendStream(Account contact) {
        return countStreamForAccount(contact, groupAccountManager.findPublicEstablished(contact), friendshipManager.findFriends(contact), postBookmarkManager.findByAccount(contact), "visitor");
    }

    public static int getCountComments(Post post) {
        return countCommentsForPost2(post.id);
    }

    public boolean belongsToGroup(Post post) {
        return post.group != null;
    }

    public boolean belongsToAccount(Post post) {
        return post.account != null;
    }

    public boolean belongsToPost(Post post) {
        return post.parent != null;
    }

    public boolean isMine(Post post) {
        return post.account.equals(post.owner);
    }

    /**
     * Collect all AccountIds, which are able to view this.post
     *
     * @return List of AccountIds
     */
    public Set<Long> findAllowedToViewAccountIds(Post post) {

        Set<Long> viewableIds = new HashSet<>();

        // everybody from post.group can see this post
        if (belongsToGroup(post)) {
            viewableIds.addAll(groupAccountManager.findAccountIdsByGroup(post.group, LinkType.establish));
        }


        if (belongsToAccount(post)) {

            // every friend from post.account can see this post
            viewableIds.addAll(friendshipManager.findFriendsId(post.account));

            // the owner of this.account can see this post
            viewableIds.add(post.account.id);

            // everybody can see his own post
            if (isMine(post)) {
                viewableIds.add(post.owner.id);
            }
        }

        // multiple options if post is a comment
        if (belongsToPost(post)) {

            // every member from post.parent.group can see this post
            if (belongsToGroup(post.parent)) {
                viewableIds.addAll(groupAccountManager.findAccountIdsByGroup(post.parent.group, LinkType.establish));
            }

            // every friend from post.parent.account can see this post
            if (belongsToAccount(post.parent)) {
                viewableIds.addAll(friendshipManager.findFriendsId(post.parent.account));

                // everybody can see his own comment
                if (isMine(post.parent)) {
                    viewableIds.add(post.owner.id);
                }
            }
        }
        return viewableIds;
    }

    public boolean isPublic(Post post) {

        // post in public group
        if (belongsToGroup(post)) {
            return post.group.groupType.equals(GroupType.open);
        }

        // comment in public group
        if (belongsToPost(post) && belongsToGroup(post.parent)) {
            return post.parent.group.groupType.equals(GroupType.open);
        }
        return false;
    }


    public long indexAllPosts() throws IOException {
        final long start = System.currentTimeMillis();
        for (Post post : allWithoutExceptionPosts()) elasticsearchService.index(post);
        return (System.currentTimeMillis() - start) / 1000;

    }

    /**
     * Get all posts except error posts (from Admin)
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<Post> allWithoutExceptionPosts() {
        Group group = groupManager.findByTitle(configuration.getString("htwplus.admin.group"));
        Account adminAccount = group.owner;
        return jpaApi.em().createQuery("FROM Post p WHERE p.owner.id != :adminId AND p.group.id != :groupId OR p.group IS NULL")
                .setParameter("groupId", group.id)
                .setParameter("adminId", adminAccount.id)
                .getResultList();
    }

    /**
     * Get all posts owned by a specific user
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Post> listAllPostsOwnedBy(Long id) {
        return jpaApi.em().createQuery("FROM Post p WHERE p.owner.id = " + id).getResultList();
    }

    /**
     * get a list of posts posted on the wall of the specified account
     */
    @SuppressWarnings("unchecked")
    public List<Post> listAllPostsPostedOnAccount(Long id) {
        return jpaApi.em().createQuery("FROM Post p WHERE p.account.id = " + id).getResultList();
    }
}
