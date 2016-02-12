package managers;

import models.Folder;
import models.Media;
import models.Studycourse;
import play.db.jpa.JPA;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by Iven on 26.12.2015.
 */
public class FolderManager implements BaseManager {

    @Inject
    MediaManager mediaManager;

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
        Folder folder = ((Folder) model);

        if (!folder.folders.isEmpty()) {
            for (Folder subFolder : folder.folders) {
                delete(subFolder);
            }
        }
        for (Media media : folder.files) {
            mediaManager.delete(media);
        }
        JPA.em().remove(folder);
    }

    public Folder findRoot(Folder folder) {
        if(folder.parent == null) return folder;
        return findRoot(folder.parent);
    }
}
