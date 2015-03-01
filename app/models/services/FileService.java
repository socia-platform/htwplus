package models.services;

import play.Play;
import play.Logger;
import play.api.PlayException;
import org.apache.commons.lang.Validate;
import eu.medsea.mimeutil.MimeUtil;
import eu.medsea.mimeutil.MimeType;
import java.io.File;
import java.util.Collection;

public class FileService {

    private String path;
    private String realm;
    private File file;

    public FileService(String realm, File file) {
        this.path = Play.application().configuration().getString("media.fileStore");
        if(this.path ==  null) {
            throw new PlayException(
                    "Configuration Error", 
                    "The configuration key 'media.fileStore' is not set");
        }
        this.realm = realm;
        this.file = file;
        Validate.notNull(this.realm, "The realm cannot be null");
        Validate.notNull(this.file, "The file cannot be null");
    }
    
    public boolean validateSize(long size) {
        long fileSize = file.length();
        if(fileSize > size){
            return false;
        } else {
            return true;
        }
    }
    
    public boolean validateExtension(String[] extensions){
        
        
        return true;
    }
    
    public String getMagicMimeType() {
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
