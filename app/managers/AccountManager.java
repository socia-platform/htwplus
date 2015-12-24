package managers;

import com.typesafe.config.ConfigFactory;
import models.*;
import models.enums.AccountRole;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.Logger;
import play.db.jpa.JPA;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * Created by Iven on 17.12.2015.
 */
public class AccountManager implements BaseManager {

    @Inject
    ElasticsearchService elasticsearchService;

    @Inject
    PostManager postManager;

    @Inject
    GroupManager groupManager;

    @Inject
    GroupAccountManager groupAccountManager;

    @Inject
    FriendshipManager friendshipManager;

    @Inject
    MediaManager mediaManager;

    @Inject
    NotificationManager notificationManager;

    @Override
    public void create(Object model) {
        Account account = (Account) model;

        account.name = account.firstname + " " + account.lastname;
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
    }

    @Override
    public void delete(Object model) {
        Account account = (Account) model;

        Account dummy = Account.findByEmail(ConfigFactory.load().getString("htwplus.dummy.mail"));

        if (dummy == null) {
            Logger.error("Couldn't delete account because there is no Dummy Account! (mail=" + ConfigFactory.load().getString("htwplus.dummy.mail") + ")");
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
        List<Friendship> friendships = Friendship.listAllFriendships(account.id);
        for (Friendship friendship : friendships) {
            friendshipManager.delete(friendship);
        }

        // Anonymize media //
        List<Media> media = Media.listAllOwnedBy(account.id);
        for (Media med : media) {
            med.owner = dummy;
            mediaManager.update(med);
        }

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
     * Try to get all accounts...
     * @return List of accounts.
     */
    @SuppressWarnings("unchecked")
    public static List<Account> all() {
        return JPA.em().createQuery("FROM Account").getResultList();
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

        return (System.currentTimeMillis() - start) / 100;

    }
}
