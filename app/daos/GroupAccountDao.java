package daos;

import models.GroupAccount;
import models.services.ElasticsearchService;
import play.db.jpa.JPA;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Created by Iven on 16.12.2015.
 */
public class GroupAccountDao {

    @Inject
    ElasticsearchService elasticsearchService;

    public void create(GroupAccount groupAccount) {
        JPA.em().persist(groupAccount);

        // each group document contains information about their member
        // if a user create or join to this.group -> (re)index this.group document
        try {
            elasticsearchService.indexGroup(groupAccount.group);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
