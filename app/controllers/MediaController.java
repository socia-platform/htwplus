package controllers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import managers.FolderManager;
import managers.MediaManager;
import models.Folder;
import models.Media;
import models.services.NotificationService;
import play.db.jpa.Transactional;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Result;
import play.mvc.Security;
import play.twirl.api.Content;

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


@Security.Authenticated(Secured.class)
public class MediaController extends BaseController {

    @Inject
    MediaManager mediaManager;

    @Inject
    FolderManager folderManager;

    final static String tempPrefix = "htwplus_temp";
    private Config conf = ConfigFactory.load();
    final int MAX_FILESIZE = conf.getInt("media.maxSize.file");

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
                    Media media = mediaManager.findById(Long.parseLong(s));
                    if (Secured.deleteMedia(media)) {
                        mediaManager.delete(media);
                    }
                }
            }

            // delete folder and files
            if (folderSelection != null) {
                for (String folderId : folderSelection) {
                    Folder folder = folderManager.findById(Long.parseLong(folderId));
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
                    Media media = mediaManager.findById(Long.parseLong(s));
                    if (Secured.viewMedia(media)) {
                        mediaList.add(media);
                    }
                }
            }

            // grab folder files
            if (folderSelection != null) {
                for (String folderId : folderSelection) {
                    Folder folder = folderManager.findById(Long.parseLong(folderId));
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
        String tmpPath = conf.getString("media.tempPath");
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
    public Result upload(Long folderId) {
        // Get the data
        MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart upload = body.getFile("file");
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
                NotificationService.getInstance().createNotification(med, Media.MEDIA_NEW_MEDIA);
            } catch (Exception e) {
                return internalServerError("Während des Uploads ist etwas schiefgegangen!");
            }
            return created("/media/"+med.id);
        } else {
            return internalServerError("Es konnte keine Datei gefunden werden!");
        }
    }
}