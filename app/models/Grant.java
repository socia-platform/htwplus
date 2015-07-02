package models;

import models.base.BaseModel;
import play.db.jpa.JPA;

import javax.persistence.*;

/**
 * Created by richard on 01.07.15.
 */
@Entity
public class Grant extends BaseModel {

    @ManyToOne
    public Account account;

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

    public static Grant findByCode(String code) {
        try{
            return (Grant) JPA.em()
                    .createQuery("from Grant a where a.ccode = :code")
                    .setParameter("code", code).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }
}
