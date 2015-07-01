package models;

import models.base.BaseModel;
import org.hibernate.annotations.ManyToAny;
import play.db.jpa.JPA;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

/**
 * Created by richard on 01.07.15.
 */

public class Grant extends BaseModel {

    public Account user;

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
}
