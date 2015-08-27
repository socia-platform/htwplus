package models;

import models.base.BaseModel;
import play.db.jpa.JPA;
import util.ExposeField;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Created by richard on 01.07.15.
 */
@Entity
public class Token extends BaseModel {

    @ManyToOne
    public Client client;

    @ManyToOne
    public Account user;

    @Column(unique=true)
    public String accessToken;

    public String refreshToken;

    public Date expires;

    public Token() {}

    public Token(Client client, Account user, Long expiresIn) {
        this.client = client;
        this.user = user;
        this.expires = new Date((Date.from(Instant.now()).getTime() + expiresIn * 1000));
        accessToken = UUID.randomUUID().toString();
        refreshToken = UUID.randomUUID().toString();
    }

    @Override
    public void create() {
        JPA.em().persist(this);
    }

    @Override
    public void update() {
        JPA.em().merge(this);
    }

    @Override
    public void delete() {

    }

    public static Token findByAccesToken(String accessToken) {
        try{
            return (Token) JPA.em()
                    .createQuery("from Token a where a.accessToken = :accessToken")
                    .setParameter("accessToken", accessToken).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    public static Token findByRefreshToken(String refreshToken) {
        try{
            return (Token) JPA.em()
                    .createQuery("from Token a where a.refreshToken = :refreshToken")
                    .setParameter("refreshToken", refreshToken).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    public boolean hasExpired() {
        if (expires != null)
            return !expires.after(Date.from(Instant.now()));
        else
            return false;
    }

    public long expiresIn() {
        if (!hasExpired()) {
            return (expires.getTime() - Date.from(Instant.now()).getTime()) / 1000;
        } else
            return -1;
    }

    public Token refresh(Long expiresIn) {
        expires = new Date((Date.from(Instant.now()).getTime() + expiresIn * 1000));
        return this;
    }
}
