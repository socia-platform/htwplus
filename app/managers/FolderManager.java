package managers;

import models.Folder;
import models.Studycourse;
import play.db.jpa.JPA;

import java.util.List;

/**
 * Created by Iven on 26.12.2015.
 */
public class FolderManager implements BaseManager {

    public Folder findById(long id) {
        return JPA.em().find(Folder.class, id);
    }

    @Override
    public void create(Object model) {
        JPA.em().persist(model);
    }

    @Override
    public void update(Object model) {
        JPA.em().merge(model);
    }

    @Override
    public void delete(Object model) {
        JPA.em().remove(model);
    }

}
