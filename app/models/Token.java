package models;

import models.base.BaseModel;
import play.db.jpa.JPA;

import javax.persistence.Entity;

/**
 * Created by richard on 01.07.15.
 */

public class Token extends BaseModel {

    public Client client;

    public Account user;

    public String accessToken;

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
