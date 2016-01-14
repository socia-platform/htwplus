package managers;

import models.Studycourse;
import play.db.jpa.JPA;

import java.util.List;

/**
 * Created by Iven on 26.12.2015.
 */
public class StudycourseManager {

    public Studycourse findById(Long id) {
        return JPA.em().find(Studycourse.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<Studycourse> getAll() {
        return JPA.em().createQuery("FROM Studycourse ORDER BY title").getResultList();
    }
}
