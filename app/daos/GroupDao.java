package daos;

import controllers.Component;
import models.Account;
import models.Group;
import models.GroupAccount;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.db.jpa.JPA;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Created by Iven on 16.12.2015.
 */
public class GroupDao {

    @Inject
    ElasticsearchService elasticsearchService;

    @Inject
    GroupAccountDao groupAccountDao;

    @Inject
    GroupAccount groupAccount;

    public void createWithGroupAccount(Group group) {
        Account currentAccount = Component.currentAccount();

        group.owner = currentAccount;
        this.create(group);

        groupAccount.account = currentAccount;
        groupAccount.group = group;
        groupAccount.linkType = LinkType.establish;
        groupAccountDao.create(groupAccount);
    }

    public void create(Group group) {
        JPA.em().persist(group);
        try {
            elasticsearchService.indexGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
