package managers;

import models.Folder;
import models.Media;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Iven on 26.12.2015.
 */
public class FolderManager implements BaseManager {

    MediaManager mediaManager;
    JPAApi jpaApi;

    @Inject
    public FolderManager(MediaManager mediaManager, JPAApi jpaApi) {
        this.mediaManager = mediaManager;
        this.jpaApi = jpaApi;
    }

    public Folder findById(long id) {
        return JPA.em().find(Folder.class, id);
    }

    @Override
    public void create(Object model) {
        jpaApi.em().persist(model);
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

    public List<Media> getAllMedia(Folder folder) {
        List<Media> mediaList = new ArrayList<>();
        mediaList = mediaManager.findByFolder(folder.id);
        if (!folder.folders.isEmpty()) {
            for (Folder subFolder : folder.folders) {
                mediaList.addAll(mediaManager.findByFolder(subFolder.id));
            }
        }
        return mediaList;
    }

    public static Folder findRoot(Folder folder) {
        if(folder.parent == null) return folder;
        return findRoot(folder.parent);
    }

    /**
     * count all files within a given folder and his subfolders.
     * @param folder
     * @return
     */
    public static int countAll(Folder folder) {
        int totalFiles = folder.files.size();
        if (!folder.folders.isEmpty()) {
            for (Folder subFolder : folder.folders) {
                totalFiles += countAll(subFolder);
            }
        }
        return totalFiles;
    }
}
