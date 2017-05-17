package managers;

import daos.GroupAccountDao;
import models.*;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Iven on 16.12.2015.
 */
public class GroupAccountManager implements BaseManager {

    ElasticsearchService elasticsearchService;
    NotificationManager notificationManager;
    PostManager postManager;
    MediaManager mediaManager;
    JPAApi jpaApi;
    GroupAccountDao groupAccountDao;

    @Inject
    public GroupAccountManager(ElasticsearchService elasticsearchService,
            NotificationManager notificationManager,
            PostManager postManager,
            MediaManager mediaManager,
            JPAApi jpaApi, GroupAccountDao groupAccountDao) {
        this.elasticsearchService = elasticsearchService;
        this.notificationManager = notificationManager;
        this.postManager = postManager;
        this.mediaManager = mediaManager;
        this.jpaApi = jpaApi;
        this.groupAccountDao = groupAccountDao;

    }

    @Override
    public void create(Object model) {
        jpaApi.em().persist(model);
        reIndex(((GroupAccount) model).group);
    }

    @Override
    public void update(Object model) {
        jpaApi.em().merge(model);
        reIndex(((GroupAccount) model).group);
    }

    @Override
    public void delete(Object model) {
        GroupAccount groupAccount = (GroupAccount) model;
        jpaApi.em().remove(groupAccount);
        reIndex(groupAccount.group);
        notificationManager.deleteReferencesForAccountId(groupAccount.group, groupAccount.account.id);
    }

    public List<GroupAccount> findRequests(Account account) {
        return groupAccountDao.findRequests(account);
    }

    public List<Group> findGroupsEstablished(Account account) {
        return groupAccountDao.findGroupsEstablished(account);
    }

    public List<Group> findCoursesEstablished(Account account) {
        return groupAccountDao.findCoursesEstablished(account);
    }

    public GroupAccount findByAccountAndGroup(Account account, Group group) {
        return groupAccountDao.find(account, group);
    }

    public List<Account> findAccountsByGroup(Group group, LinkType type) {
        return groupAccountDao.findAccountsByGroup(group, type);
    }

    /**
     * each group document contains information about their member
     * if a user gets access to this group -> (re)index group document
     * and (re)index all containing post documents
     *
     * @param group group which should be indexed
     */
    private void reIndex(Group group) {
        new Thread(() -> {
            jpaApi.withTransaction(() -> {
                try {
                    elasticsearchService.indexGroup(group, groupAccountDao.findAccountIdsByGroup(group, LinkType.establish));
                    for (Post post : postManager.getPostsForGroup(group, 0, 0)) {
                        elasticsearchService.indexPost(post, postManager.isPublic(post), postManager.findAllowedToViewAccountIds(post));
                    }

                    for (Media medium : mediaManager.findByFolder(group.rootFolder.id)) {
                        elasticsearchService.indexMedium(medium, mediaManager.isPublic(medium), mediaManager.findAllowedToViewAccountIds(medium));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }

    /**
     * filter GroupAccounts by LinkType
     * @param groupAccountList list of groupAccounts
     * @param linkType to filter
     * @return Accounts
     */
    public static List<Account> filterGroupAccountsByLinkType(Set<GroupAccount> groupAccountList, LinkType linkType) {
        List<Account> accountList = new ArrayList<>();
        for (GroupAccount groupAccount : groupAccountList) {
            if (groupAccount.linkType.equals(linkType)) {
                accountList.add(groupAccount.account);
            }
        }
        return accountList;
    }
}
