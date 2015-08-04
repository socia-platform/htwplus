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

    @ExposeField(name = "client")
    @ManyToOne
    public Client client;

    @ExposeField(name = "user")
    @ManyToOne
    public Account user;

    @ExposeField(name = "access_token")
    @Column(unique=true)
    public String accessToken;

    @ExposeField(name = "refresh_token")
    public String refreshToken;

    @ExposeField(name = "expiration_date")
    public Date expires;

    public Token() {}

    public Token(Client client, Account user, Date expires) {
        this.client = client;
        this.user = user;
        this.expires = expires;
        accessToken = UUID.randomUUID().toString();
        refreshToken = UUID.randomUUID().toString();
    }

    @Override
    public void create() {
        JPA.em().persist(this);
    }

    @Override
    public void update() {

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

    public boolean hasExpired() {
        if (expires != null)
            return expires.after(Date.from(Instant.now()));
        else
            return false;
    }
}
