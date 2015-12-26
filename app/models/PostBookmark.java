package models;

import models.base.BaseModel;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class PostBookmark extends BaseModel {

    @ManyToOne
    public Account owner;

    @ManyToOne
    public Post post;

    public PostBookmark() {
    }

    public PostBookmark(Account owner, Post post) {
        this.owner = owner;
        this.post = post;
    }
}