package managers;

import models.Avatar;
import models.base.FileOperationException;
import models.base.ValidationException;
import models.enums.AvatarSize;
import models.services.FileService;
import models.services.ImageService;
import play.Configuration;
import play.i18n.Messages;
import play.mvc.Http;

import javax.inject.Inject;
import java.io.File;

public class AvatarManager {


    @Inject
    Configuration configuration;


    static public String AVATAR_REALM = "avatar";
    static public int AVATAR_MIN_SIZE = 250;
    static public int AVATAR_MAX_SIZE = 4000;
    static public int AVATAR_LARGE_SIZE = 600;
    static public int AVATAR_MEDIUM_SIZE = 140;
    static public int AVATAR_SMALL_SIZE = 70;
    static public String AVATAR_CUSTOM = "custom";

    /**
     * Set the temporary avatar image for the user
     *
     * @param filePart The uploaded file part
     * @throws ValidationException
     */
    public void setTempAvatar(Http.MultipartFormData.FilePart filePart, Long modelId) throws ValidationException {
        FileService fileService;
        fileService = new FileService(AVATAR_REALM, filePart, configuration.getString("media.fileStore"));


        int maxSize = configuration.getInt("avatar.maxSize");
        if (!fileService.validateSize(FileService.MBAsByte(maxSize))) {
            throw new ValidationException(Messages.get("error.fileToBig"));
        }
        String[] allowedContentTypes = {FileService.MIME_JPEG, FileService.MIME_PNG};
        if (!fileService.validateContentType(allowedContentTypes)) {
            throw new ValidationException(Messages.get("error.contentTypeNotSupported"));
        }
        if (!ImageService.validateMinSize(fileService.getFile(), AVATAR_MIN_SIZE, AVATAR_MIN_SIZE)) {
            throw new ValidationException(Messages.get("error.resolutionLow"));
        }
        if (!ImageService.validateMaxSize(fileService.getFile(), AVATAR_MAX_SIZE, AVATAR_MAX_SIZE)) {
            throw new ValidationException(Messages.get("error.resolutionHigh"));
        }

        fileService.saveFile(this.getTempAvatarName(modelId), true);
    }

    /**
     * Returns the temporary avatar image
     *
     * @return The temp avatar
     */
    public File getTempAvatar(Long modelId) {
        FileService fileService;
        try {
            fileService = new FileService(AVATAR_REALM, this.getTempAvatarName(modelId), configuration.getString("media.fileStore"));
            return fileService.getFile();
        } catch (FileOperationException e) {
            return null;
        }
    }

    /**
     * Saves the avatar
     *
     * @param avatarForm
     */
    public void saveAvatar(Avatar avatarForm, Long modelId) throws FileOperationException {
        FileService fsTempAvatar = new FileService(AVATAR_REALM, this.getTempAvatarName(modelId), configuration.getString("media.fileStore"));
        FileService fsAvatarLarge = fsTempAvatar.copy(this.getAvatarName(AvatarSize.LARGE, modelId));
        ImageService.crop(fsAvatarLarge.getFile(), avatarForm.x, avatarForm.y, avatarForm.width, avatarForm.height);
        FileService fsAvatarMedium = fsAvatarLarge.copy(this.getAvatarName(AvatarSize.MEDIUM, modelId));
        FileService fsAvatarSmall = fsAvatarLarge.copy(this.getAvatarName(AvatarSize.SMALL, modelId));
        ImageService.resize(fsAvatarLarge.getFile(), AVATAR_LARGE_SIZE, AVATAR_LARGE_SIZE);
        ImageService.resize(fsAvatarMedium.getFile(), AVATAR_MEDIUM_SIZE, AVATAR_MEDIUM_SIZE);
        ImageService.resize(fsAvatarSmall.getFile(), AVATAR_SMALL_SIZE, AVATAR_SMALL_SIZE);
    }

    /**
     * Get the avatar in different sizes
     *
     * @param size
     * @returns
     */
    public File getAvatar(AvatarSize size, Long modelId) {
        FileService fileService;
        try {
            fileService = new FileService(AvatarManager.AVATAR_REALM, this.getAvatarName(size, modelId), configuration.getString("media.fileStore"));
            return fileService.getFile();
        } catch (FileOperationException e) {
            return null;
        }
    }

    /**
     * Get the file name for an avatar in different sizes
     *
     * @param size
     * @param modelId account- or group-model id
     * @return
     */
    private String getAvatarName(AvatarSize size, Long modelId) {
        switch (size) {
            case SMALL:
                return modelId.toString() + "_small.jpg";
            case MEDIUM:
                return modelId.toString() + "_medium.jpg";
            case LARGE:
                return modelId.toString() + "_large.jpg";
        }
        return modelId.toString() + "_large.jpg";
    }

    /**
     * Get the temp avatar name
     *
     * @param modelId account- or group-model id
     * @return
     */
    private String getTempAvatarName(Long modelId) {
        String fileName = modelId.toString() + ".jpg";
        return fileName;
    }

}
