package models;

import models.base.BaseModel;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.List;

@Entity
public class Studycourse extends BaseModel {

    @Required
    @Column(length = 2000)
    public String title;
}