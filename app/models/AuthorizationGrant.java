package models;

import models.base.BaseModel;
import play.db.jpa.JPA;

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
public class AuthorizationGrant extends BaseModel {

    @ManyToOne
    public Account user;

    @ManyToOne
    public Client client;

    public String code;

    public Date expires;

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

    public AuthorizationGrant() {}

    public AuthorizationGrant(Account user, Client client, long expiresIn) {
        this.user = user;
        this.client = client;
        this.expires = new Date(Date.from(Instant.now()).getTime() + expiresIn * 1000);
        code = UUID.randomUUID().toString();
    }

    public static AuthorizationGrant findByCode(String code) {
        try{
            return (AuthorizationGrant) JPA.em()
                    .createQuery("from AuthorizationGrant a where a.code = :code")
                    .setParameter("code", code).getSingleResult();
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

}
