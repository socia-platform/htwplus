package models.services;

import play.Play;
import play.Logger;
import play.api.PlayException;
import org.apache.commons.lang.Validate;
import eu.medsea.mimeutil.MimeUtil;
import eu.medsea.mimeutil.MimeType;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.api.libs.MimeTypes;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;


public class FileService {
    
    public static String MIME_JPEG = "image/jpeg";

    private String path;
    private String realm;
    private File file;
    private FilePart filePart;

    public FileService(String realm) {
        this.path = Play.application().configuration().getString("media.fileStore");
        if(this.path ==  null) {
            throw new PlayException(
                    "Configuration Error", 
                    "The configuration key 'media.fileStore' is not set");
        }
        this.realm = realm;
        Validate.notNull(this.realm, "The realm cannot be null");
    }
    
    public void setFilePart(FilePart filePart){
        this.filePart = filePart;
        this.file = this.filePart.getFile();
        Validate.notNull(this.file, "The file cannot be null");
    }
    
    public boolean validateSize(long size) {
        Validate.notNull(this.file, "The file property is null.");
        long fileSize = file.length();
        if(fileSize > size){
            return false;
        } else {
            return true;
        }
    }
    
    public boolean validateContentType(String[] contentTypes){
        MimeTypes.defaultTypes();
        String type = this.filePart.getContentType();
        if(Arrays.asList(contentTypes).contains(type)) {
            return true;
        } else {
            return false;
        }
    }
    
    public String getMagicMimeType() {
        Validate.notNull(this.file, "The file property is null.");
        MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
        Collection<MimeType> mimeTypes = MimeUtil.getMimeTypes(file);
        String result;
        if(!mimeTypes.isEmpty()){
            result = mimeTypes.iterator().next().toString();
        } else {
            result = null;
        }

        Logger.info(result);
        return result;
    }

    public void saveFile(File file, String fileName) {
        this.saveFile(fileName, false);
    }

    public void saveFile(String fileName, boolean overwrite) {
        Validate.notNull(this.file, "The file property is null.");
        String path = this.buildPath(fileName);
        Logger.info(path);
        File newFile = new File(path);
        
        if(overwrite) {
            if(newFile.exists()) {
                newFile.delete();
            }
        }

        newFile.getParentFile().mkdirs();
        boolean result = this.file.renameTo(newFile);

        if(!result) {
            throw new PlayException(
                    "File Error",
                    "The file could not be stored");
        }
    }
    
    public File openFile(String fileName){
        String path = this.buildPath(fileName);
        File file = new File(path);
        if(file.exists()){
            return file;
        } else {
            return null;
        }
    }
    
    private String buildPath(String fileName) {
        return this.path + "/" + this.realm + "/" + fileName;
    }

    public static long MBAsByte(long size) {
        return (size * 1024 * 1024);
    }

    public static long KBAsByte(long size) {
        return (size * 1024);
    }
    
}
