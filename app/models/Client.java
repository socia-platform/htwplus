package models;

import models.base.BaseModel;
import play.db.jpa.JPA;

import javax.persistence.Entity;
import javax.persistence.NoResultException;
import java.net.URI;

/**
 * Created by richard on 01.07.15.
 */
@Entity
public class Client extends BaseModel {

    public String clientName;
    public String clientId;
    public String clientSecret;
    public URI callback;

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

    public static Client findById(Long id) {
        return JPA.em().find(Client.class, id);
    }

    public static Client findByClientId(String clientId) {
        try{
            return (Client) JPA.em()
                    .createQuery("from Client a where a.clientId = :clientId")
                    .setParameter("clientId", clientId).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }
}
