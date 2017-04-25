package controllers;

import managers.FolderManager;
import managers.MediaManager;
import models.Folder;
import models.Media;
import models.services.NotificationService;
import play.Configuration;
import play.Logger;
import play.db.jpa.Transactional;
import play.mvc.Http.MultipartFormData;
import play.mvc.Result;
import play.mvc.Security;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import views.html.Media.list;


@Security.Authenticated(Secured.class)
public class MediaController extends BaseController {

    final Logger.ALogger LOG = Logger.of(MediaController.class);

    MediaManager mediaManager;
    FolderManager folderManager;
    Configuration configuration;
    NotificationService notificationService;

    @Inject
    public MediaController(MediaManager mediaManager,
            FolderManager folderManager,
            Configuration configuration, NotificationService notificationService) {
        this.mediaManager = mediaManager;
        this.folderManager = folderManager;
        this.configuration = configuration;
        this.notificationService = notificationService;

        this.MAX_FILESIZE = configuration.getInt("media.maxSize.file");

    }

    final static String tempPrefix = "htwplus_temp";
    int MAX_FILESIZE;

    @Transactional(readOnly = true)
    public Result view(Long mediaId, String action) {
        Media media = mediaManager.findById(mediaId);
        if (media == null) {
            return notFound();
        }
        if (Secured.viewMedia(media)) {
            switch (action) {
                case "show":
                    response().setHeader("Content-Disposition", "inline; filename=\"" + media.fileName + "\"");
                    break;
                case "download":
                    response().setHeader("Content-Disposition", "attachment; filename=\"" + media.fileName + "\"");
                    break;
            }
            return ok(media.file);
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
            return Secured.nullRedirect(request());
        }
    }

    public Result mediaList(Long folderId) {
        Folder folder = folderManager.findById(folderId);

        if (!Secured.viewFolder(folder)) {
            return forbidden("Dazu hast du keine Berechtigung");
        }
        Folder rootFolder  = FolderManager.findRoot(folder);
        List<Media> mediaSet = folder.files;
        List<Folder> folderList = folder.folders;

        for (Media media : mediaSet) {
            media.sizeInByte = mediaManager.bytesToString(media.size, false);
        }

        return ok(list.render(mediaSet, folderList, rootFolder.group.id));
    }



    @Transactional
    public Result delete(Long id) {
        Media media = mediaManager.findById(id);

        if (media == null) {
            return notFound();
        }

        if (!Secured.deleteMedia(media)) {
            return redirect(controllers.routes.Application.index());
        }

        mediaManager.delete(media);
        flash("success", "Datei " + media.title + " erfolgreich gelöscht!");
        return Secured.nullRedirect(request());
    }

    @Transactional(readOnly = true)
    public Result multiView() {

        String[] action = request().body().asFormUrlEncoded().get("action");
        Result ret = Secured.nullRedirect(request());
        Media media;
        Folder folder;

        String[] mediaselection = request().body().asFormUrlEncoded().get("mediaSelection");
        String[] folderSelection = request().body().asFormUrlEncoded().get("folderSelection");

        if (mediaselection == null && folderSelection == null) {
            flash("error", "Bitte wähle mindestens eine Datei aus.");
            return ret;
        }

        if (action[0].equals("delete")) {

            // delete media files
            if (mediaselection != null) {
                for (String s : mediaselection) {
                    try {
                        media = mediaManager.findById(Long.parseLong(s));
                    } catch (NumberFormatException nfe) {
                        Logger.error("Unable to parse media id: " + s, nfe);
                        return ret;
                    }

                    if (Secured.deleteMedia(media)) {
                        mediaManager.delete(media);
                    }
                }
            }

            // delete folder and files
            if (folderSelection != null) {
                for (String folderId : folderSelection) {
                    try {
                        folder = folderManager.findById(Long.parseLong(folderId));
                    } catch (NumberFormatException nfe) {
                        Logger.error("Unable to parse folder id: " + folderId, nfe);
                        return ret;
                    }

                    if (Secured.deleteFolder(folder)) {
                        folderManager.delete(folder);
                    }
                }
            }
            flash("success", "Datei(en) erfolgreich gelöscht!");
        }

        if (action[0].equals("download")) {

            String filename = createFileName("HTWplus");
            List<Media> mediaList = new ArrayList<>();

            // grab media files
            if (mediaselection != null) {
                for (String s : mediaselection) {
                    try {
                        media = mediaManager.findById(Long.parseLong(s));
                    } catch (NumberFormatException nfe) {
                        Logger.error("Unable to parse media id: " + s, nfe);
                        return ret;
                    }

                    if (Secured.viewMedia(media)) {
                        mediaList.add(media);
                    }
                }
            }

            // grab folder files
            if (folderSelection != null) {
                for (String folderId : folderSelection) {
                    try {
                        folder = folderManager.findById(Long.parseLong(folderId));
                    } catch (NumberFormatException nfe) {
                        Logger.error("Unable to parse folder id: " + folderId, nfe);
                        return ret;
                    }

                    if (Secured.viewFolder(folder)) {
                        mediaList.addAll(folderManager.getAllMedia(folder));
                    }
                }
            }

            try {
                File file = createZIP(mediaList);
                response().setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
                return ok(file);
            } catch (IOException e) {
                flash("error", "Etwas ist schiefgegangen. Bitte probiere es noch einmal!");
                return ret;
            }
        }
        return ret;
    }

    private String createFileName(String prefix) {
        return prefix + "-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".zip";
    }


    private File createZIP(List<Media> media) throws IOException {

        //cleanUpTemp(); // JUST FOR DEVELOPMENT, DO NOT USE IN PRODUCTION
        String tmpPath = configuration.getString("media.tempPath");
        File file = File.createTempFile(tempPrefix, ".tmp", new File(tmpPath));

        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file));
        zipOut.setLevel(Deflater.NO_COMPRESSION);
        byte[] buffer = new byte[4092];
        int byteCount = 0;
        for (Media m : media) {
            zipOut.putNextEntry(new ZipEntry(m.fileName));
            FileInputStream fis = new FileInputStream(m.file);
            byteCount = 0;
            while ((byteCount = fis.read(buffer)) != -1) {
                zipOut.write(buffer, 0, byteCount);
            }
            fis.close();
            zipOut.closeEntry();
        }

        zipOut.flush();
        zipOut.close();
        return file;
    }

    /**
     * Upload some media.
     *
     * @param folderId Folder to upload.
     * @return Result
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public Result upload(Long folderId) {
        // Get the data
        MultipartFormData body = request().body().asMultipartFormData();
        MultipartFormData.FilePart<File> upload = body.getFile("file");
        Folder folder = folderManager.findById(folderId);

        if (!Secured.viewFolder(folder)) {
            return forbidden("Dazu hast du keine Berechtigung");
        }

        if (upload != null) {
            // Create the Media models and perform some checks
            // File too big?
            if (mediaManager.byteAsMB(upload.getFile().length()) > MAX_FILESIZE) {
                return status(REQUEST_ENTITY_TOO_LARGE, "Es sind maximal "+ MAX_FILESIZE + " MByte pro Datei möglich.");
            }
            // File already exists?
            if (mediaManager.existsInFolder(upload.getFilename(), folder)) {
                return status(CONFLICT, "Eine Datei mit diesem Namen existiert bereits.");
            }
            // Everything is fine
            Media med = new Media();
            med.title = upload.getFilename();
            med.mimetype = upload.getContentType();
            med.fileName = upload.getFilename();
            med.file = upload.getFile();
            med.owner = Component.currentAccount();
            med.folder = folder;
            med.temporarySender = Component.currentAccount();

            // Persist medialist and create notification(s)
            try {
                mediaManager.create(med);
                notificationService.createNotification(med, Media.MEDIA_NEW_MEDIA);
                LOG.info("New media " + med.fileName + " from " + med.owner.id + " in folder " + med.folder.id);
            } catch (Exception e) {
                return internalServerError("Während des Uploads ist etwas schiefgegangen!");
            }

            return created("/media/" + med.id);
        } else {
            return internalServerError("Es konnte keine Datei gefunden werden!");
        }
    }
}