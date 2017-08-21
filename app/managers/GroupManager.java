package managers;

import daos.GroupAccountDao;
import daos.GroupDao;
import models.*;
import models.base.FileOperationException;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import sun.awt.image.ImageWatched;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * Created by Iven on 16.12.2015.
 */
public class GroupManager implements BaseManager {

    ElasticsearchService elasticsearchService;
    GroupAccountManager groupAccountManager;
    PostManager postManager;
    NotificationManager notificationManager;
    FolderManager folderManager;
    AvatarManager avatarManager;
    JPAApi jpaApi;
    GroupAccountDao groupAccountDao;
    GroupDao groupDao;

    @Inject
    public GroupManager(ElasticsearchService elasticsearchService,
            GroupAccountManager groupAccountManager,
            PostManager postManager,
            NotificationManager notificationManager,
            FolderManager folderManager,
            AvatarManager avatarManager,
            JPAApi jpaApi, GroupAccountDao groupAccountDao, GroupDao groupDao) {
        this.elasticsearchService = elasticsearchService;
        this.groupAccountManager = groupAccountManager;
        this.postManager = postManager;
        this.notificationManager = notificationManager;
        this.folderManager = folderManager;
        this.avatarManager = avatarManager;
        this.jpaApi = jpaApi;
        this.groupAccountDao = groupAccountDao;
        this.groupDao = groupDao;
    }


    public void createWithGroupAccount(Group group, Account account) {
        group.owner = account;
        group.rootFolder = new Folder("_"+group.title, account, null, group, null);
        folderManager.create(group.rootFolder);
        create(group);
        groupAccountManager.create(new GroupAccount(group, account, LinkType.establish));
    }

    @Override
    public void create(Object model) {
        jpaApi.em().persist(model);
    }

    @Override
    public void update(Object model) {
        Group group = ((Group) model);
        JPA.em().merge(group);
        try {
            elasticsearchService.indexGroup(group, groupAccountDao.findAccountIdsByGroup(group, LinkType.establish));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Object model) {
        Group group = ((Group) model);

        // delete all Posts
        List<Post> posts = postManager.getPostsForGroup(group, 0, 0);
        for (Post post : posts) {
            postManager.delete(post);
        }

        //delete root folder
        folderManager.delete(group.rootFolder);

        // Delete Notifications
        notificationManager.deleteReferences(group);

        // Delete Elasticsearch document
        elasticsearchService.delete(group);

        jpaApi.em().remove(group);
    }

    public Group findById(Long id) {
        return groupDao.findById(id);
    }

    public Group findByTitle(String title) {
        return groupDao.findByTitle(title);
    }

    public List<Group> listAllGroupsOwnedBy(Long id) {
        return groupDao.listAllGroupsOwnedBy(id);
    }

    /**
     * Returns true, if an account is member of a group.
     *
     * @param group   Group instance
     * @param account Account instance
     * @return True, if account is member of group
     */
    public static boolean isMember(Group group, Account account) {
        @SuppressWarnings("unchecked")
        List<GroupAccount> groupAccounts = (List<GroupAccount>) JPA
                .em()
                .createQuery(
                        "SELECT g FROM GroupAccount g WHERE g.account.id = ?1 and g.group.id = ?2 AND linkType = ?3")
                .setParameter(1, account.id).setParameter(2, group.id)
                .setParameter(3, LinkType.establish).getResultList();

        return !groupAccounts.isEmpty();
    }

    public long indexAllGroups() throws IOException {
        final long start = System.currentTimeMillis();
        for (Group group : groupDao.all()) elasticsearchService.indexGroup(group, groupAccountDao.findAccountIdsByGroup(group, LinkType.establish));
        return (System.currentTimeMillis() - start) / 1000;

    }

    public void saveAvatar(Avatar avatar, Group group) throws FileOperationException {
        avatarManager.saveAvatar(avatar, group.id);
        group.hasAvatar = true;
        this.update(group);
    }
}
