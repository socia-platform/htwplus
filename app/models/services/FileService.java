package models.services;

import models.base.FileOperationException;
import play.Configuration;
import play.Logger;
import play.api.PlayException;
import org.apache.commons.lang.Validate;
import eu.medsea.mimeutil.MimeUtil;
import eu.medsea.mimeutil.MimeType;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.api.libs.MimeTypes;

import javax.inject.Inject;
import java.io.IOException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;

/**
 * This class acts as a abstraction level for file system operations.
 * It encapsulates all basic functions, like creating, reading, copying
 * moving and deleting files.
 * 
 * @Todo Not completed yet
 */
public class FileService {

    public static String MIME_JPEG = "image/jpeg";
    public static String MIME_PNG = "image/png";

    private String path;
    private String realm;
    private File file;
    private String contentType;
    private String fileName;
    private FilePart filePart;

    /**
     * Get a FileService from a basic File
     *
     * @param realm Namespace of the file
     * @param file The File
     */
    public FileService(String realm, File file, String path){
        this.initPath(realm, path);
        this.initProperties(file);
    }

    /**
     * Get a FileService from a Play FilePart
     *
     * @param realm Namespace of the file
     * @param filePart The FilePart
     */
    public FileService(String realm, Http.MultipartFormData.FilePart<File> filePart, String path){
        this(realm, filePart.getFile(), path);
        this.contentType = filePart.getContentType();
        this.fileName = filePart.getFilename();
    }

    /**
     * Get a FileService from a file name
     *
     * @param realm Namespace of the file
     * @param fileName The file name
     */
    public FileService(String realm, String fileName, String path) throws FileOperationException {
        this.initPath(realm, path);
        File file = this.openFile(fileName);
        if(file == null) {
            throw new FileOperationException("File does not exit");
        }
        this.initProperties(file);
    }

    /**
     * Initiates the path of the files by the realm
     *
     * @param realm The namespace
     */
    private void initPath(String realm, String path) {
        this.path = path;
        if(this.path ==  null) {
            throw new PlayException(
                    "Configuration Error",
                    "The configuration key 'media.fileStore' is not set");
        }
        this.realm = realm;
        Validate.notNull(this.realm, "The realm cannot be null");
    }

    /**
     * Sets the basic properties
     *
     * @param file The file
     */
    private void initProperties(File file) {
        this.file = file;
        Validate.notNull(this.file, "The file cannot be null");
    }

    /**
     * Opens a file by file name
     *
     * @param fileName The file name
     * @return File
     */
    private File openFile(String fileName){
        String path = this.buildPath(fileName);
        File file = new File(path);
        if(file.exists()){
            return file;
        } else {
            return null;
        }
    }

    /**
     * Return the actual file
     *
     * @return File
     */
    public File getFile(){
        return this.file;
    }

    /**
     * Validates the size of the file
     *
     * @param size Max Site in Byte
     * @return boolean True if valid
     */
    public boolean validateSize(long size) {
        Validate.notNull(this.file, "The file property is null.");
        long fileSize = file.length();
        if(fileSize > size){
            return false;
        } else {
            return true;
        }
    }

    /**
     * Validates the content type of the file if set
     * If not set try to set it with guessContentType()
     *
     * @param contentTypes Array if allowed content types
     * @return boolean True if valid
     */
    public boolean validateContentType(String[] contentTypes){
        Validate.notNull(this.contentType, "Content type is not set");
        MimeTypes.defaultTypes();
        if(Arrays.asList(contentTypes).contains(this.contentType)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Try to determine the content type with the Mime Type Detection utility.
     * If successful the contentType property is set automatically.
     *
     * @return Content Tyoe
     */
    public String guessContentType() {
        Validate.notNull(this.file, "The file property is null.");
        MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
        Collection<MimeType> mimeTypes = MimeUtil.getMimeTypes(file);
        String result;
        if(!mimeTypes.isEmpty()){
            result = mimeTypes.iterator().next().toString();
        } else {
            result = null;
        }
        this.contentType = result;
        return result;
    }

    /**
     * Save the file to a custom file name within the realm.
     *
     * @param fileName The name of the file
     * @param overwrite Set to true if already existing file should be overwritten
     */
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

        try {
            java.nio.file.Files.move(this.file.toPath(), newFile.toPath());
        }
        catch (IOException e) {
            throw new PlayException(
                    "File Error",
                    "The file could not be stored");
        }
    }

    /**
     * Copy the file and get a new FileService for the new file
     *
     * @param destFileName The destination file name
     * @return FileService
     */
    public FileService copy(String destFileName){
        String destPath = this.buildPath(destFileName);
        File destFile = new File(destPath);
        try {
            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return null;
        }

        if(destFile.exists()){
            return new FileService(this.realm, destFile, path);
        } else {
            return null;
        }
    }

    /**
     * Build the actual path for the current file
     *
     * @param fileName The name of the file
     * @return String
     */
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
