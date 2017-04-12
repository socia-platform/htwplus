package managers;

import models.Account;
import models.Folder;
import models.Media;
import models.enums.GroupType;
import models.enums.LinkType;
import models.services.ElasticsearchService;
import play.Configuration;
import play.Logger;
import play.db.jpa.JPAApi;

import java.io.IOException;
import java.nio.file.Files;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Iven on 17.12.2015.
 */
public class MediaManager implements BaseManager {


    @Inject
    NotificationManager notificationManager;

    @Inject
    Configuration configuration;

    @Inject
    GroupAccountManager groupAccountManager;

    @Inject
    ElasticsearchService elasticsearchService;

    @Inject
    JPAApi jpaApi;

    final String tempPrefix = "htwplus_temp";

    @Override
    public void create(Object model) {
        Media media = (Media) model;
        media.size = media.file.length();
        media.url = createRelativeURL() + "/" + getUniqueFileName(media.fileName);
        try {
            createFile(media);
            jpaApi.em().persist(media);
            elasticsearchService.index(media);
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
        jpaApi.em().merge(model);
    }

    @Override
    public void delete(Object model) {
        Media media = (Media) model;

        try {
            // remove deprecated notifications
            notificationManager.deleteReferences(media);

            deleteFile(media);
            jpaApi.em().remove(media);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Media> findAll() {
        return jpaApi.em().createQuery("FROM Media").getResultList();
    }

    public Media findById(Long id) {
        Media media = jpaApi.em().find(Media.class, id);
        if (media == null) {
            return null;
        }
        String path = configuration.getString("media.path");
        media.file = new File(path + "/" + media.url);
        if (media.file.exists()) {
            return media;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Media> findByFolder(Long folderId) {
        List<Media> mediaList = jpaApi.em().createQuery("FROM Media m WHERE m.folder.id = " + folderId).getResultList();
        List<Media> returningMediaList = new ArrayList<>();
        for (Media media : mediaList) {
            String path = configuration.getString("media.path");
            media.file = new File(path + "/" + media.url);
            if (media.file.exists()) {
                returningMediaList.add(media);
            }
        }
        return returningMediaList;
    }

    @SuppressWarnings("unchecked")
    public List<Media> listAllOwnedBy(Long id) {
        return jpaApi.em().createQuery("FROM Media m WHERE m.owner.id = " + id).getResultList();
    }

    public boolean existsInFolder(String mediaTitle, Folder folder) {
        List<Media> mediaList = findByFolder(folder.id);
        for (Media media : mediaList) {
            if (media.title.equals(mediaTitle)) {
                return true;
            }
        }
        return false;
    }


    public boolean isOwner(Long mediaId, Account account) {
        Media m = jpaApi.em().find(Media.class, mediaId);
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
        String path = configuration.getString("media.path");
        File file = new File(path + "/" + media.url);
        if (file.exists()) {
            file.delete();
        } else {
            throw new FileNotFoundException("File does not exist.");
        }
    }

    private void createFile(Media media) throws Exception {
        String path = configuration.getString("media.path");
        File newFile = new File(path + "/" + media.url);
        if (newFile.exists()) {
            throw new Exception("File exists already");
        }
        newFile.getParentFile().mkdirs();
        Files.move(media.file.toPath(), newFile.toPath());
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

        String tmpPath = configuration.getString("media.tempPath");
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

    public int byteAsMB(long size) {
        return (int) (size / 1024 / 1024);
    }

    public long indexAllMedia() throws IOException {
        final long start = System.currentTimeMillis();
        for (Media medium : findAll()) elasticsearchService.index(medium);
        return (System.currentTimeMillis() - start) / 1000;

    }

    /**
     * Collect all AccountIds, which are able to view this medium
     *
     * @return List of AccountIds
     */
    public Set<Long> findAllowedToViewAccountIds(Media medium) {

        Set<Long> viewableIds = new HashSet<>();

        Folder rootFolder = medium.findRoot();

        // medium belongs to account
        if (rootFolder.account != null) {
            viewableIds.add(rootFolder.owner.id);
        }

        // medium belongs to group
        if(rootFolder.group != null) {
            viewableIds.addAll(groupAccountManager.findAccountIdsByGroup(rootFolder.group, LinkType.establish));
        }

        return viewableIds;
    }

    public boolean isPublic(Media medium) {
        if(medium.findGroup() != null) {
            return medium.findGroup().groupType.equals(GroupType.open);
        }
        return false;
    }
}
