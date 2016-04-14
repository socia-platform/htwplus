package controllers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import managers.FolderManager;
import managers.GroupManager;
import managers.MediaManager;
import models.Folder;
import models.Group;
import models.Media;
import models.services.NotificationService;
import org.apache.commons.collections.ListUtils;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Group.view;

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
    GroupManager groupManager;

    @Inject
    FolderManager folderManager;

    final static String tempPrefix = "htwplus_temp";
    private Config conf = ConfigFactory.load();

    final int MAX_FILESIZE_TOTAL = conf.getInt("media.maxSize.total");
    final int MAX_FILESIZE = conf.getInt("media.maxSize.file");

    @Transactional(readOnly = true)
    public Result view(Long mediaId, String action) {
        Media media = mediaManager.findById(mediaId);
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


        Call ret = controllers.routes.Application.index();
        if (media.folder != null) {
            Long folderId = media.folder.id;
            Long groupId = media.findGroup().id;

            if (!Secured.deleteMedia(media)) {
                return redirect(controllers.routes.Application.index());
            }
            ret = routes.GroupController.media(groupId, folderId);
        }

        mediaManager.delete(media);
        flash("success", "Datei " + media.title + " erfolgreich gelöscht!");
        return redirect(ret);
    }

    @Transactional(readOnly = true)
    public Result multiView(String target, Long id) {

        String[] action = request().body().asFormUrlEncoded().get("action");
        Call ret = controllers.routes.Application.index();
        Group group = null;

        if (target.equals(Media.GROUP)) {
            group = groupManager.findById(id);
            if (!Secured.viewGroup(group)) {
                return redirect(controllers.routes.Application.index());
            }
            ret = controllers.routes.GroupController.media(id, 0L);
        } else {
            return redirect(ret);
        }

        String[] mediaselection = request().body().asFormUrlEncoded().get("mediaSelection");
        String[] folderSelection = request().body().asFormUrlEncoded().get("folderSelection");

        if (mediaselection == null && folderSelection == null) {
            flash("error", "Bitte wähle mindestens eine Datei aus.");
            return redirect(ret);
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
        }

        if (action[0].equals("download")) {

            String filename = createFileName(group.title);
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
                File file = createZIP(mediaList, filename);
                response().setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
                return ok(file);
            } catch (IOException e) {
                flash("error", "Etwas ist schiefgegangen. Bitte probiere es noch einmal!");
                return redirect(ret);
            }
        }
        return redirect(ret);
    }

    private String createFileName(String prefix) {
        return prefix + "-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".zip";
    }


    private File createZIP(List<Media> media, String fileName) throws IOException {

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
     * Frontend route.
     *
     * @param folderId folder which to upload media
     * @return routes to group media
     */
    public Result groupUpload(Long folderId) {

        Folder folder = folderManager.findById(folderId);

        if (!Secured.viewFolder(folder)) {
            flash("error", "Dazu hast du keine Berechtigung");
            return Secured.nullRedirect(request());
        }

        Group group = folderManager.findRoot(folder).group;

        Result uploadResult = upload(folderId);

        if (uploadResult.status() == REQUEST_ENTITY_TOO_LARGE) {
            flash("error", "Es sind maximal " + MAX_FILESIZE + " MByte pro Datei & " + MAX_FILESIZE_TOTAL + " MByte pro Upload möglich!");
        }
        if (uploadResult.status() == INTERNAL_SERVER_ERROR) {
            flash("error", "Während des Uploads ist etwas schiefgegangen!");
        }
        if (uploadResult.status() == CONFLICT) {
            flash("error", "Eine Datei mit dem Namen existiert bereits");
        }
        if (uploadResult.status() == OK) {
            flash("success", "Datei(en) erfolgreich hinzugefügt.");
        }

        return redirect(controllers.routes.GroupController.media(group.id, folderId));
    }

    /**
     * Upload some media.
     *
     * @param folderId Folder to upload.
     * @return Result
     */
    @Transactional
    public Result upload(Long folderId) {

        // Is it to big in total?
        String[] contentLength = request().headers().get("Content-Length");
        if (contentLength != null) {
            int size = Integer.parseInt(contentLength[0]);
            if (mediaManager.byteAsMB(size) > MAX_FILESIZE_TOTAL) {
                return status(REQUEST_ENTITY_TOO_LARGE);
            }
        } else {
            return internalServerError();
        }

        // Get the data
        MultipartFormData body = request().body().asMultipartFormData();
        List<Http.MultipartFormData.FilePart> uploads = body.getFiles();
        List<Media> mediaList = new ArrayList<Media>();
        Folder folder = folderManager.findById(folderId);

        if (!uploads.isEmpty()) {
            // Create the Media models and perform some checks
            for (FilePart upload : uploads) {
                // File too big?
                if (mediaManager.byteAsMB(upload.getFile().length()) > MAX_FILESIZE) {
                    return status(REQUEST_ENTITY_TOO_LARGE);
                }
                // File already exists?
                if (mediaManager.existsInFolder(upload.getFilename(), folder)) {
                    return status(CONFLICT);
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
                mediaList.add(med);
            }

            // Persist medialist and create notification(s)
            for (Media media : mediaList) {
                try {
                    mediaManager.create(media);
                    NotificationService.getInstance().createNotification(media, Media.MEDIA_NEW_MEDIA);
                } catch (Exception e) {
                    return internalServerError(e.getMessage());
                }
            }

            return ok();
        } else {
            return internalServerError();
        }
    }
}