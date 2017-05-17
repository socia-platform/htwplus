package daos;

import models.Group;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by Iven on 08.05.2017.
 */
public class GroupDao {

    JPAApi jpaApi;

    @Inject
    public GroupDao(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public Group findById(Long id) {
        return jpaApi.em().find(Group.class, id);
    }

    @SuppressWarnings("unchecked")
    public Group findByTitle(String title) {
        List<Group> groups = (List<Group>) jpaApi.em()
                .createQuery("FROM Group g WHERE g.title = ?1")
                .setParameter(1, title).getResultList();

        if (groups.isEmpty()) {
            return null;
        } else {
            return groups.get(0);
        }
    }

    @SuppressWarnings("unchecked")
    public static Group findByTitle2(String title) {
        List<Group> groups = (List<Group>) JPA.em()
                .createQuery("FROM Group g WHERE g.title = ?1")
                .setParameter(1, title).getResultList();

        if (groups.isEmpty()) {
            return null;
        } else {
            return groups.get(0);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Group> all() {
        return jpaApi.em().createQuery("FROM Group").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Group> listAllGroupsOwnedBy(Long id) {
        return jpaApi.em().createQuery("FROM Group g WHERE g.owner.id = " + id).getResultList();
    }
}
