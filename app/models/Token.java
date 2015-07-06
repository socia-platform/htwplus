package models;

import models.base.BaseModel;
import play.db.jpa.JPA;
import util.Expose;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import java.util.Date;
import java.util.UUID;

/**
 * Created by richard on 01.07.15.
 */
@Entity
public class Token extends BaseModel {

    @Expose(name = "client")
    @ManyToOne
    public Client client;

    @Expose(name = "user")
    @ManyToOne
    public Account user;

    @Expose(name = "access_token")
    @Column(unique=true)
    public String accessToken;

    @Expose(name = "refresh_token")
    public String refreshToken;

    @Expose(name = "expiration_date")
    public Date expires;

    public Token() {}

    public Token(Client client, Account User, Date expires) {
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
}
