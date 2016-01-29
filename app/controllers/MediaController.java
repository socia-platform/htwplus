package controllers;

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

import managers.FolderManager;
import models.Folder;
import models.services.NotificationService;
import org.apache.commons.io.FileUtils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import managers.GroupManager;
import managers.MediaManager;

import models.Group;
import models.Media;
import models.services.NotificationService;
import org.apache.commons.io.FileUtils;
import play.Logger;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.Security;

import javax.inject.Inject;


@Security.Authenticated(Secured.class)
public class MediaController extends BaseController {

    @Inject
    MediaManager mediaManager;

    @Inject
    GroupManager groupManager;

    @Inject
    FolderManager folderManager;

    static Form<Media> mediaForm = Form.form(Media.class);
    final static String tempPrefix = "htwplus_temp";
    private Config conf = ConfigFactory.load();

    @Transactional(readOnly = true)
    public Result view(Long id) {
        Media media = mediaManager.findById(id);
        if (Secured.viewMedia(media)) {
            if (media == null) {
                return redirect(controllers.routes.Application.index());
            } else {
                response().setHeader("Content-disposition", "attachment; filename=\"" + media.fileName + "\"");
                return ok(media.file);
            }
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
            return redirect(controllers.routes.Application.index());
        }
    }

    @Transactional
    public Result delete(Long id) {
        Media media = mediaManager.findById(id);

        Call ret = controllers.routes.Application.index();
        if (media.group != null) {
            Group group = media.group;
            if (!Secured.deleteMedia(media)) {
                return redirect(controllers.routes.Application.index());
            }
            ret = controllers.routes.GroupController.media(group.id, 0L);
        }

        mediaManager.delete(media);
        flash("success", "Datei " + media.title + " erfolgreich gelöscht!");
        return redirect(ret);
    }

    @Transactional(readOnly = true)
    public Result multiView(String target, Long id) {

        Call ret = controllers.routes.Application.index();
        Group group = null;
        String filename = "result.zip";

        if (target.equals(Media.GROUP)) {
            group = groupManager.findById(id);
            if (!Secured.viewGroup(group)) {
                return redirect(controllers.routes.Application.index());
            }
            filename = createFileName(group.title);
            ret = controllers.routes.GroupController.media(id, 0L);
        } else {
            return redirect(ret);
        }

        String[] selection = request().body().asFormUrlEncoded().get("selection");
        List<Media> mediaList = new ArrayList<Media>();

        if (selection != null) {
            for (String s : selection) {
                Media media = mediaManager.findById(Long.parseLong(s));
                if (Secured.viewMedia(media)) {
                    mediaList.add(media);
                } else {
                    flash("error", "Dazu hast du keine Berechtigung!");
                    return redirect(controllers.routes.Application.index());
                }
            }
        } else {
            flash("error", "Bitte wähle mindestens eine Datei aus.");
            return redirect(ret);
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
     * New file is uploaded.
     *
     * @param target Target of the file (e.g. "group")
     * @return Result
     */
    @Transactional
    public Result upload(String target, Long folderId) {
        final int maxTotalSize = conf.getInt("media.maxSize.total");
        final int maxFileSize = conf.getInt("media.maxSize.file");

        Call ret = controllers.routes.Application.index();
        Group group;
        Folder folder = folderManager.findById(folderId);

        // Where to put the media
        if (target.equals(Media.GROUP)) {
            group = groupManager.findById(folder.findRoot(folder).group.id);
            if (!Secured.uploadMedia(group)) {
                return redirect(controllers.routes.Application.index());
            }
            ret = controllers.routes.GroupController.media(group.id, folderId);
        } else {
            return redirect(ret);
        }

        // Is it to big in total?
        String[] contentLength = request().headers().get("Content-Length");
        if (contentLength != null) {
            int size = Integer.parseInt(contentLength[0]);
            if (mediaManager.byteAsMB(size) > maxTotalSize) {
                flash("error", "Du darfst auf einmal nur " + maxTotalSize + " MB hochladen.");
                return redirect(ret);
            }
        } else {
            flash("error", "Etwas ist schiefgegangen. Bitte probiere es noch einmal!");
            return redirect(ret);
        }

        // Get the data
        MultipartFormData body = request().body().asMultipartFormData();
        List<Http.MultipartFormData.FilePart> uploads = body.getFiles();

        List<Media> mediaList = new ArrayList<Media>();

        if (!uploads.isEmpty()) {

            // Create the Media models and perform some checks
            for (FilePart upload : uploads) {

                Media med = new Media();
                med.title = upload.getFilename();
                med.mimetype = upload.getContentType();
                med.fileName = upload.getFilename();
                med.file = upload.getFile();
                med.owner = Component.currentAccount();

                if (mediaManager.byteAsMB(med.file.length()) > maxFileSize) {
                    flash("error", "Die Datei " + med.title + " ist größer als " + maxFileSize + " MB!");
                    return redirect(ret);
                }

                String error = "Eine Datei mit dem Namen " + med.title + " existiert bereits";
                if (target.equals(Media.GROUP)) {
                    med.folder = folder;
                    med.temporarySender = Component.currentAccount();
                    if (mediaManager.existsInGroup(med, group)) {
                        flash("error", error);
                        return redirect(ret);
                    }

                }
                mediaList.add(med);
            }

            for (Media m : mediaList) {
                try {
                    mediaManager.create(m);

                    // create group notification, if a group exists
                    if (m.group != null) {
                        NotificationService.getInstance().createNotification(m, Media.MEDIA_NEW_MEDIA);
                    }
                } catch (Exception e) {
                    return internalServerError(e.getMessage());
                }
            }
            flash("success", "Datei(en) erfolgreich hinzugefügt.");
            return redirect(ret);
        } else {
            flash("error", "Etwas ist schiefgegangen. Bitte probiere es noch einmal!");
            return redirect(ret);
        }
    }
}