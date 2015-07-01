package models;

import models.base.BaseModel;
import play.db.jpa.JPA;

import javax.persistence.Entity;
import java.net.URI;

/**
 * Created by richard on 01.07.15.
 */
@Entity
public class Client extends BaseModel {

    public String clientId;
    public String clientSecret;
    public URI callBack;

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
