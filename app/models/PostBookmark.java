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
}