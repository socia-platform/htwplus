package managers;

import com.typesafe.config.Config;
import controllers.Component;
import daos.GroupAccountDao;
import daos.GroupDao;
import models.*;
import models.base.FileOperationException;
import models.enums.AccountRole;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.Logger;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.io.IOException;
import java.util.List;

/**
 * Created by Iven on 17.12.2015.
 */
public class AccountManager implements BaseManager {

    ElasticsearchService elasticsearchService;
    PostManager postManager;
    GroupDao groupDao;
    GroupAccountDao groupAccountDao;
    GroupManager groupManager;
    FriendshipManager friendshipManager;
    MediaManager mediaManager;
    NotificationManager notificationManager;
    AvatarManager avatarManager;
    FolderManager folderManager;
    Config configuration;
    JPAApi jpaApi;

    @Inject
    public AccountManager(ElasticsearchService elasticsearchService,
                          PostManager postManager,
                          GroupDao groupDao,
                          GroupAccountDao groupAccountDao, GroupManager groupManager,
                          FriendshipManager friendshipManager,
                          MediaManager mediaManager,
                          NotificationManager notificationManager,
                          AvatarManager avatarManager,
                          FolderManager folderManager, Config configuration, JPAApi jpaApi) {
        this.elasticsearchService = elasticsearchService;
        this.postManager = postManager;
        this.groupDao = groupDao;
        this.groupAccountDao = groupAccountDao;
        this.groupManager = groupManager;
        this.friendshipManager = friendshipManager;
        this.mediaManager = mediaManager;
        this.notificationManager = notificationManager;
        this.avatarManager = avatarManager;
        this.folderManager = folderManager;
        this.configuration = configuration;
        this.jpaApi = jpaApi;
    }

    @Override
    public void create(Object model) {
        Account account = (Account) model;

        account.name = account.firstname + " " + account.lastname;
        account.rootFolder = new Folder("_" + account.name, account, null, null, account);
        folderManager.create(account.rootFolder);

        jpaApi.em().persist(account);
        try {
            elasticsearchService.indexAccount(account, friendshipManager.findFriendsId(account));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Object model) {
        Account account = (Account) model;

        account.name = account.firstname + " " + account.lastname;
        jpaApi.em().merge(account);
        try {
            elasticsearchService.indexAccount(account, friendshipManager.findFriendsId(account));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Object model) {
        Account account = (Account) model;

        Account dummy = findByEmail(configuration.getString("htwplus.dummy.mail"));

        if (dummy == null) {
            Logger.error("Couldn't delete account because there is no Dummy Account! (mail=" + configuration.getString("htwplus.dummy.mail") + ")");
            throw new RuntimeException("Couldn't delete account because there is no Dummy Account!");
        }

        // Anonymize Posts //
        List<Post> owned = postManager.listAllPostsOwnedBy(account.id);
        for (Post post : owned) {
            post.owner = dummy;
            postManager.create(post); // elastic search indexing
            postManager.update(post);
        }
        List<Post> pinned = postManager.listAllPostsPostedOnAccount(account.id);
        for (Post post : pinned) {
            post.account = dummy;
            postManager.create(post); // elastic search indexing
            postManager.update(post);
        }

        // Anonymize created groups //
        List<Group> groups = groupDao.listAllGroupsOwnedBy(account.id);
        for (Group group : groups) {
            if (groupAccountDao.findAccountsByGroup(group, LinkType.establish).size() == 1) { // if the owner is the only member of the group
                Logger.info("Group '" + group.title + "' is now empty, so it will be deleted!");
                groupManager.delete(group);
            } else {
                group.owner = dummy;
                groupManager.update(group);
            }
        }

        // Delete Friendships //
        List<Friendship> friendships = friendshipManager.listAllFriendships(account.id);
        for (Friendship friendship : friendships) {
            friendshipManager.delete(friendship);
        }

        // Anonymize media //
        List<Media> media = mediaManager.listAllOwnedBy(account.id);
        for (Media med : media) {
            med.owner = dummy;
            mediaManager.update(med);
        }

        // transfer root folder (change it to a subfolder for our dummy account)
        Folder rootFolder = account.rootFolder;
        rootFolder.account = null;
        rootFolder.parent = dummy.rootFolder;
        rootFolder.owner = dummy;

        // Delete incoming notifications //
        notificationManager.deleteNotificationsForAccount(account.id);

        // Delete outgoing notifications //
        List<Notification> notifications = notificationManager.findBySenderId(account.id);
        for (Notification not : notifications) {
            notificationManager.delete(not);
        }

        elasticsearchService.delete(account);

        jpaApi.em().remove(account);
    }

    /**
     * Returns an account by account ID.
     *
     * @param id Account ID
     * @return Account instance
     */
    public Account findById(Long id) {
        return jpaApi.em().find(Account.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<Account> findAll() {
        return jpaApi.em().createQuery("SELECT a FROM Account a ORDER BY a.name").getResultList();
    }

    /**
     * Retrieve a User from email.
     */
    public Account findByEmail(String email) {
        if (email.isEmpty()) {
            return null;
        }
        try {
            return (Account) jpaApi.em()
                    .createQuery("from Account a where a.email = :email")
                    .setParameter("email", email).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    /**
     * Retrieve a User by loginname
     */
    public Account findByLoginName(String loginName) {
        try {
            return (Account) jpaApi.em()
                    .createQuery("from Account a where a.loginname = :loginname")
                    .setParameter("loginname", loginName).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    public boolean isAccountValid(String email, String password) {
        try {
            final Account result = (Account) jpaApi.em()
                    .createQuery("from Account a where a.email = :email")
                    .setParameter("email", email).getSingleResult();
            if (result != null && Component.md5(password).equals(result.password)) {
                return true;
            }
        } catch (NoResultException exp) {
            return false;
        }
        return false;
    }

    /**
     * Try to get all accounts...
     *
     * @return List of accounts.
     */
    @SuppressWarnings("unchecked")
    public List<Account> all() {
        return jpaApi.withTransaction(() -> {
            return jpaApi.em().createQuery("FROM Account").getResultList();
        });
    }

    /**
     * Returns a list of account instances by an ID collection of Strings.
     *
     * @param accountIds String array of account IDs
     * @return List of accounts
     */
    public List<Account> getAccountListByIdCollection(final List<String> accountIds) {
        StringBuilder joinedAccountIds = new StringBuilder();
        for (int i = 0; i < accountIds.size(); i++) {
            if (i > 0) {
                joinedAccountIds.append(",");
            }
            joinedAccountIds.append(accountIds.get(i));
        }

        return jpaApi.em()
                .createQuery("FROM Account a WHERE a.id IN (" + joinedAccountIds.toString() + ")", Account.class)
                .getResultList();
    }

    /**
     * Index the current account
     */
    public void indexAccount(Account account) {
        try {
            elasticsearchService.indexAccount(account, friendshipManager.findFriendsId(account));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Index all accounts
     */
    public long indexAllAccounts() throws IOException {
        final long start = System.currentTimeMillis();

        for (Account account : all()) {
            if (account.role != AccountRole.DUMMY)
                elasticsearchService.indexAccount(account, friendshipManager.findFriendsId(account));
        }

        return (System.currentTimeMillis() - start) / 1000;

    }

    public void saveAvatar(Avatar avatar, Account account) throws FileOperationException {
        avatarManager.saveAvatar(avatar, account.id);
        account.avatar = AvatarManager.AVATAR_CUSTOM;
        this.update(account);
    }

}
