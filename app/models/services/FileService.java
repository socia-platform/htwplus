package models.services;

import play.Play;
import play.Logger;
import play.api.PlayException;
import org.apache.commons.lang.Validate;

import java.io.File;

public class FileService {

    private String path;
    private String realm;

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
    
    public void saveFile(File file, String fileName) {
        String path = this.buildPath(fileName);
        Logger.info(path);
        File newFile = new File(path);
        newFile.getParentFile().mkdirs();
        boolean result = file.renameTo(newFile);
        
        if(!result) {
            throw new PlayException(
                    "File Error",
                    "The file could not be stored");
        }
    }
    
    private String buildPath(String fileName) {
        return this.path + "/" + this.realm + "/" + fileName;
    }
}
