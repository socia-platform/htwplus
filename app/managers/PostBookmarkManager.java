package managers;

import models.Account;
import models.Post;
import models.PostBookmark;
import play.db.jpa.JPA;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by Iven on 25.12.2015.
 */
public class PostBookmarkManager implements BaseManager {
    @Override
    public void create(Object object) {
        JPA.em().persist(object);
    }

    @Override
    public void update(Object object) {

    }

    @Override
    public void delete(Object object) {
        JPA.em().remove(object);
    }

    public PostBookmark findById(Long id) {
        return JPA.em().find(PostBookmark.class, id);
    }

    public PostBookmark findByAccountAndPost(Account account, Post post) {
        try {
            return (PostBookmark) JPA.em().createQuery("SELECT pl FROM PostBookmark pl WHERE pl.owner.id = :accountId AND pl.post.id = :postId")
                    .setParameter("accountId", account.id)
                    .setParameter("postId", post.id)
                    .getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    public static boolean isPostBookmarkedByAccount(Account account, Post post) {
        Query query = JPA.em().createQuery("SELECT COUNT(pl) FROM PostBookmark pl WHERE pl.owner.id = :accountId AND pl.post.id = :postId")
                .setParameter("accountId", account.id)
                .setParameter("postId", post.id);

        if(((Number) query.getSingleResult()).intValue() > 0)
            return true;

        return false;
    }

    @SuppressWarnings("unchecked")
    public List<Post> findByAccount(Account account) {
        return JPA.em().createQuery("SELECT pl.post FROM PostBookmark pl WHERE pl.owner.id = :accountId")
                .setParameter("accountId", account.id)
                .getResultList();
    }
}
