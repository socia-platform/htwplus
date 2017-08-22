package managers;

import com.google.inject.Inject;
import models.Studycourse;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;

import java.util.List;

/**
 * Created by Iven on 26.12.2015.
 */
public class StudycourseManager {

    JPAApi jpaApi;

    @Inject
    public StudycourseManager(JPAApi jpaApi) {
        this.jpaApi = jpaApi;
    }

    public Studycourse findById(Long id) {
        return jpaApi.em().find(Studycourse.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<Studycourse> getAll() {
        return JPA.em().createQuery("FROM Studycourse ORDER BY title").getResultList();
    }
}
