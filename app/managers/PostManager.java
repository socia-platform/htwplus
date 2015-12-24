package managers;

import models.Group;
import models.Notification;
import models.Post;
import models.enums.AccountRole;
import models.services.ElasticsearchService;
import play.db.jpa.JPA;

import javax.inject.Inject;
import javax.persistence.Query;
import java.io.IOException;
import java.util.List;

/**
 * Created by Iven on 17.12.2015.
 */
public class PostManager implements BaseManager {

    @Inject
    ElasticsearchService elasticsearchService;

    @Override
    public void create(Object model) {
        Post post = (Post) model;

        try {
            if (!post.owner.role.equals(AccountRole.ADMIN)) {
                elasticsearchService.index(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JPA.em().persist(post);
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

        Notification.deleteReferences(post);

        JPA.em().remove(post);

        // Delete Elasticsearch document
        elasticsearchService.delete(post);
    }

    public List<Post> getCommentsForPost(Long id, int limit, int offset) {
        Query query = JPA.em()
                .createQuery("SELECT p FROM Post p WHERE p.parent.id = ?1 ORDER BY p.createdAt ASC")
                .setParameter(1, id);

        query = limit(query, limit, offset);

        return (List<Post>) query.getResultList();
    }

    public List<Post> getPostsForGroup(final Group group, final int limit, final int page) {
        Query query = JPA.em()
                .createQuery("SELECT p FROM Post p WHERE p.group.id = ?1 ORDER BY p.createdAt DESC")
                .setParameter(1, group.id);

        int offset = (page * limit) - limit;
        query = limit(query, limit, offset);

        return query.getResultList();
    }

    private Query limit(Query query, int limit, int offset) {
        query.setMaxResults(limit);
        if (offset >= 0) {
            query.setFirstResult(offset);
        }
        return query;
    }

    public long indexAllPosts() throws IOException {
        final long start = System.currentTimeMillis();
        for (Post post: allWithoutAdmin()) elasticsearchService.index(post);
        return (System.currentTimeMillis() - start) / 100;

    }

    /**
     * Get all posts except error posts (from Admin)
     * @return
     */
    public List<Post> allWithoutAdmin() {
        return JPA.em().createQuery("FROM Post p WHERE p.owner.id != 1").getResultList();
    }

    /**
     * Get all posts owned by a specific user
     * @return
     */
    public List<Post> listAllPostsOwnedBy(Long id) {
        return JPA.em().createQuery("FROM Post p WHERE p.owner.id = " + id).getResultList();
    }

    /**
     * get a list of posts posted on the wall of the specified account
     */
    public List<Post> listAllPostsPostedOnAccount(Long id) {
        return JPA.em().createQuery("FROM Post p WHERE p.account.id = " + id).getResultList();
    }
}
