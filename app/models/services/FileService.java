package models.services;

import play.Play;
import play.Logger;

import java.io.File;

public class FileService {
    private static FileService instance = new FileService();

    private String path;

    public static FileService getInstance() {
        return instance;
    }

    private FileService() {
        this.path = Play.application().configuration().getString("media.path");
    }

    public void saveFile(File file) {
        String path = this.path + "/" + "test.jpg";
        Logger.info(path);
        File newFile = new File(path);
        file.renameTo(newFile);
    }
}
