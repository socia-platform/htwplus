package managers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import models.Account;
import models.Group;
import models.Media;
import org.apache.commons.io.FileUtils;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Iven on 17.12.2015.
 */
public class MediaManager implements BaseManager {

    @Inject
    NotificationManager notificationManager;

    private Config conf = ConfigFactory.load();
    final String tempPrefix = "htwplus_temp";

    @Override
    public void create(Object model) {
        Media media = (Media) model;
        media.size = media.file.length();
        media.url = createRelativeURL() + "/" + getUniqueFileName(media.fileName);
        try {
            createFile(media);
            JPA.em().persist(media);
        } catch (Exception e) {
            try {
                throw e;
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void update(Object model) {
        JPA.em().merge(model);
    }

    @Override
    public void delete(Object model) {
        Media media = (Media) model;

        try {
            // remove deprecated notifications
            notificationManager.deleteReferences(media);

            deleteFile(media);
            JPA.em().remove(media);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Media findById(Long id) {
        Media media = JPA.em().find(Media.class, id);
        if (media == null) {
            return null;
        }
        String path = Play.application().configuration().getString("media.path");
        media.file = new File(path + "/" + media.url);
        if (media.file.exists()) {
            return media;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Media> listAllOwnedBy(Long id) {
        return JPA.em().createQuery("FROM Media m WHERE m.owner.id = " + id).getResultList();
    }

    public boolean existsInGroup(Media media, Group group) {
        List<Media> mediaList = group.media;
        for (Media m : mediaList) {
            if (m.title.equals(media.title)) {
                return true;
            }
        }
        return false;
    }


    public boolean isOwner(Long mediaId, Account account) {
        Media m = JPA.em().find(Media.class, mediaId);
        if (m.owner.equals(account)) {
            return true;
        } else {
            return false;
        }
    }

    private String getUniqueFileName(String fileName) {
        return UUID.randomUUID().toString() + '_' + fileName;
    }

    private void deleteFile(Media media) throws FileNotFoundException {
        String path = Play.application().configuration().getString("media.path");
        File file = new File(path + "/" + media.url);
        if (file.exists()) {
            file.delete();
        } else {
            throw new FileNotFoundException("File does not exist.");
        }
    }

    private void createFile(Media media) throws Exception {
        String path = Play.application().configuration().getString("media.path");
        File newFile = new File(path + "/" + media.url);
        if (newFile.exists()) {
            throw new Exception("File exists already");
        }
        newFile.getParentFile().mkdirs();
        media.file.renameTo(newFile);
        if (!newFile.exists()) {
            throw new Exception("Could not upload file");
        }
    }

    private String createRelativeURL() {
        Date now = new Date();
        String format = new SimpleDateFormat("yyyy/MM/dd").format(now);
        return format;
    }

    public String bytesToString(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Cleans the temporary media directoy used for ZIP Downloads
     */
    public void cleanUpTemp() {
        Logger.info("Cleaning the Tempory Media Directory");

        String tmpPath = conf.getString("media.tempPath");
        File dir = new File(tmpPath);
        Logger.info("Directory: " + dir.toString());
        File[] files = dir.listFiles();
        Logger.info("Absolut Path: " + dir.getAbsolutePath());

        if (files != null) {
            // Just delete files older than 1 hour
            long hours = 1;
            long eligibleForDeletion = System.currentTimeMillis() - (hours * 60 * 60 * 1000L);

            Logger.info("Found " + files.length + " Files");
            if (files != null) {
                for (File file : files) {
                    Logger.info("Working on " + file.getName());
                    if (file.getName().startsWith(tempPrefix) && file.lastModified() < eligibleForDeletion) {
                        Logger.info("Deleting: " + file.getName());
                        file.delete();
                    }
                }
            }
        } else {
            Logger.info("files is null");
        }

    }

    /**
     * Size of temporary media directoy used for ZIP Downloads
     */
    public long sizeTemp() {
        String tmpPath = conf.getString("media.tempPath");
        File dir = new File(tmpPath);
        return FileUtils.sizeOfDirectory(dir);
    }

    public int byteAsMB(long size) {
        return (int) (size / 1024 / 1024);
    }
}
