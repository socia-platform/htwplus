package models;

import models.base.BaseModel;

import play.db.jpa.JPA;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

@Entity
public class PostBookmark extends BaseModel {

    @ManyToOne
    public Account owner;

    @ManyToOne
    public Post post;

    public PostBookmark() {}

    public PostBookmark(Account owner, Post post) {
        this.owner = owner;
        this.post = post;
    }

	@Override
	public void create() {
        JPA.em().persist(this);
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete() { JPA.em().remove(this); }
	
	public static PostBookmark findById(Long id) {
		return JPA.em().find(PostBookmark.class, id);
	}

    public static PostBookmark findByAccountAndPost(Account account, Post post) {
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

    public static List<Post> findByAccount(Account account) {
        return JPA.em().createQuery("SELECT pl.post FROM PostBookmark pl WHERE pl.owner.id = :accountId")
                .setParameter("accountId", account.id)
                .getResultList();
    }
}