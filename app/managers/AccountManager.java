package managers;

import com.typesafe.config.ConfigFactory;
import controllers.Component;
import models.*;
import models.base.FileOperationException;
import models.enums.AccountRole;
import models.enums.AvatarSize;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import models.services.FileService;
import play.Configuration;
import play.Logger;
import play.db.jpa.JPA;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by Iven on 17.12.2015.
 */
public class AccountManager implements BaseManager {

    ElasticsearchService elasticsearchService;
    PostManager postManager;
    GroupManager groupManager;
    GroupAccountManager groupAccountManager;
    FriendshipManager friendshipManager;
    MediaManager mediaManager;
    NotificationManager notificationManager;
    AvatarManager avatarManager;
    FolderManager folderManager;
    Configuration configuration;

    @Inject
    public AccountManager(ElasticsearchService elasticsearchService,
            PostManager postManager,
            GroupManager groupManager,
            GroupAccountManager groupAccountManager,
            FriendshipManager friendshipManager,
            MediaManager mediaManager,
            NotificationManager notificationManager,
            AvatarManager avatarManager,
            FolderManager folderManager,
            Configuration configuration) {
            this.elasticsearchService = elasticsearchService;
        this.postManager = postManager;
        this.groupManager = groupManager;
        this.groupAccountManager = groupAccountManager;
        this.friendshipManager = friendshipManager;
        this.mediaManager = mediaManager;
        this.notificationManager = notificationManager;
        this.avatarManager = avatarManager;
        this.folderManager = folderManager;
        this.configuration = configuration;
    }

    @Override
    public void create(Object model) {
        Account account = (Account) model;

        account.name = account.firstname + " " + account.lastname;
        account.rootFolder = new Folder("_"+account.name, account, null, null, account);
        folderManager.create(account.rootFolder);

        JPA.em().persist(account);
        try {
            elasticsearchService.index(account);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Object model) {
        Account account = (Account) model;

        account.name = account.firstname + " " + account.lastname;
        JPA.em().merge(account);
        try {
            elasticsearchService.index(account);
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
        List<Group> groups = groupManager.listAllGroupsOwnedBy(account.id);
        for (Group group : groups) {
            if (groupAccountManager.findAccountsByGroup(group, LinkType.establish).size() == 1) { // if the owner is the only member of the group
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

        JPA.em().remove(account);
    }

    /**
     * Returns an account by account ID.
     *
     * @param id Account ID
     * @return Account instance
     */
    public Account findById(Long id) {
        return JPA.em().find(Account.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<Account> findAll(){
        return JPA.em().createQuery("SELECT a FROM Account a ORDER BY a.name").getResultList();
    }

    /**
     * Retrieve a User from email.
     */
    public Account findByEmail(String email) {
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
    public Account findByLoginName(String loginName) {
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
    public Account findByName(String name) {
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

    /**
     * Try to get all accounts...
     * @return List of accounts.
     */
    @SuppressWarnings("unchecked")
    public List<Account> all() {
        return JPA.em().createQuery("FROM Account").getResultList();
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

        return JPA.em()
                .createQuery("FROM Account a WHERE a.id IN (" + joinedAccountIds.toString() + ")", Account.class)
                .getResultList();
    }

    /**
     * Index the current account
     */
    public void indexAccount(Account account) {
        try {
            elasticsearchService.index(account);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Index all accounts
     */
    public long indexAllAccounts() throws IOException {
        final long start = System.currentTimeMillis();

        for (Account account: all()) {
            if(account.role != AccountRole.DUMMY)
                elasticsearchService.index(account);
        }

        return (System.currentTimeMillis() - start) / 1000;

    }

    public void saveAvatar(Avatar avatar, Account account) throws FileOperationException {
        avatarManager.saveAvatar(avatar, account.id);
        account.avatar = AvatarManager.AVATAR_CUSTOM;
        this.update(account);
    }

}
