package models;

import models.base.BaseModel;
import play.db.jpa.JPA;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;

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

    public static AuthorizationGrant findByCode(String code) {
        try{
            return (AuthorizationGrant) JPA.em()
                    .createQuery("from AuthorizationGrant a where a.code = :code")
                    .setParameter("code", code).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }
}
