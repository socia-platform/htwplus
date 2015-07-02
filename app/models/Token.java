package models;

import models.base.BaseModel;
import play.db.jpa.JPA;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import java.util.Date;

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
