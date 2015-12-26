package models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigFactory;
import models.base.BaseModel;
import models.base.FileOperationException;
import models.base.IJsonNodeSerializable;
import models.base.ValidationException;
import models.enums.AccountRole;
import models.enums.EmailNotifications;
import models.services.AvatarService;
import models.services.ElasticsearchService;
import models.services.FileService;
import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.URL;
import play.Logger;
import play.data.validation.Constraints;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Http.MultipartFormData;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
public class Account extends BaseModel implements IJsonNodeSerializable {

    private final static Logger.ALogger logger = Logger.of(Account.class);

    @Inject
    public transient ElasticsearchService elasticsearchService;

    public String loginname;

    public String name;

    @Required
    public String firstname;

    @Required
    public String lastname;

    @Email
    @Column(unique = true)
    public String email;

    @Required
    public String password;

    public String avatar;

    @Type(type = "org.hibernate.type.TextType")
    public String about;

    @URL(message = "error.homepage")
    public String homepage;

    @OneToMany(mappedBy = "account", orphanRemoval = true)
    public Set<Friendship> friends;

    @OneToMany(mappedBy = "account", orphanRemoval = true)
    public Set<GroupAccount> groupMemberships;

    public Date lastLogin;

    public String studentId;

    @OneToOne
    public Studycourse studycourse;
    public String degree;
    public Integer semester;

    public AccountRole role;

    public EmailNotifications emailNotifications;

    public Integer dailyEmailNotificationHour;

    public Boolean approved;


    static public String AVATAR_REALM = "avatar";
    static public int AVATAR_MIN_SIZE = 250;
    static public int AVATAR_MAX_SIZE = 4000;
    static public int AVATAR_LARGE_SIZE = 600;
    static public int AVATAR_MEDIUM_SIZE = 140;
    static public int AVATAR_SMALL_SIZE = 70;
    static public String AVATAR_CUSTOM = "custom";

    static public enum AVATAR_SIZE {
        SMALL, MEDIUM, LARGE
    }

    /**
     * Set the temporary avatar image for the user
     *
     * @param filePart The uploaded file part
     * @throws ValidationException
     */
    public void setTempAvatar(MultipartFormData.FilePart filePart) throws ValidationException {
        FileService fileService = new FileService(Account.AVATAR_REALM, filePart);

        int maxSize = ConfigFactory.load().getInt("avatar.maxSize");
        if (!fileService.validateSize(FileService.MBAsByte(maxSize))) {
            throw new ValidationException(Messages.get("error.fileToBig"));
        }
        String[] allowedContentTypes = {FileService.MIME_JPEG, FileService.MIME_PNG};
        if (!fileService.validateContentType(allowedContentTypes)) {
            throw new ValidationException(Messages.get("error.contentTypeNotSupported"));
        }
        if (!AvatarService.validateMinSize(fileService.getFile(), Account.AVATAR_MIN_SIZE, Account.AVATAR_MIN_SIZE)) {
            throw new ValidationException(Messages.get("error.resolutionLow"));
        }
        if (!AvatarService.validateMaxSize(fileService.getFile(), Account.AVATAR_MAX_SIZE, Account.AVATAR_MAX_SIZE)) {
            throw new ValidationException(Messages.get("error.resolutionHigh"));
        }

        fileService.saveFile(this.getTempAvatarName(), true);
    }

    /**
     * Returns the temporary avatar image
     *
     * @return The temp avatar
     */
    public File getTempAvatar() {
        FileService fileService;
        try {
            fileService = new FileService(Account.AVATAR_REALM, this.getTempAvatarName());
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
    public void saveAvatar(AvatarForm avatarForm) throws FileOperationException {
        try {
            FileService fsTempAvatar = new FileService(Account.AVATAR_REALM, this.getTempAvatarName());
            FileService fsAvatarLarge = fsTempAvatar.copy(this.getAvatarName(AVATAR_SIZE.LARGE));
            AvatarService.crop(fsAvatarLarge.getFile(), avatarForm.x, avatarForm.y, avatarForm.width, avatarForm.height);
            FileService fsAvatarMedium = fsAvatarLarge.copy(this.getAvatarName(AVATAR_SIZE.MEDIUM));
            FileService fsAvatarSmall = fsAvatarLarge.copy(this.getAvatarName(AVATAR_SIZE.SMALL));
            AvatarService.resize(fsAvatarLarge.getFile(), AVATAR_LARGE_SIZE, AVATAR_LARGE_SIZE);
            AvatarService.resize(fsAvatarMedium.getFile(), AVATAR_MEDIUM_SIZE, AVATAR_MEDIUM_SIZE);
            AvatarService.resize(fsAvatarSmall.getFile(), AVATAR_SMALL_SIZE, AVATAR_SMALL_SIZE);
            this.avatar = AVATAR_CUSTOM;
        } catch (FileOperationException e) {
            logger.error(e.getMessage(), e);
            throw new FileOperationException("Error while saving avatar.");
        }
    }

    /**
     * Get the avatar in different sizes
     *
     * @param size
     * @returns
     */
    public File getAvatar(AVATAR_SIZE size) {
        FileService fileService;
        try {
            fileService = new FileService(Account.AVATAR_REALM, this.getAvatarName(size));
            return fileService.getFile();
        } catch (FileOperationException e) {
            return null;
        }
    }

    /**
     * Get the file name for an avatar in different sizes
     *
     * @param size
     * @return
     */
    private String getAvatarName(AVATAR_SIZE size) {
        switch (size) {
            case SMALL:
                return this.id.toString() + "_small.jpg";
            case MEDIUM:
                return this.id.toString() + "_medium.jpg";
            case LARGE:
                return this.id.toString() + "_large.jpg";
        }
        return this.id.toString() + "_large.jpg";
    }

    /**
     * Get the temp avatar name
     *
     * @return
     */
    private String getTempAvatarName() {
        String fileName = this.id.toString() + ".jpg";
        return fileName;
    }

    /**
     * Determines if the user has a custom avatar
     *
     * @return
     */
    public boolean hasAvatar() {
        if (this.avatar.equals(AVATAR_CUSTOM)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the initials of the user as an alternative to the avatar
     *
     * @return
     */
    public String getInitials() {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(this.firstname.charAt(0)));
        sb.append(Character.toUpperCase(this.lastname.charAt(0)));
        return sb.toString();
    }

    @Override
    public ObjectNode getAsJson() {
        ObjectNode node = Json.newObject();
        node.put("id", this.id);
        node.put("name", this.name);

        return node;
    }

    @SuppressWarnings("unchecked")
    public static List<Account> getAllNames() {
        return JPA.em().createQuery("SELECT a.id, a.name FROM Account a").getResultList();
    }

    static public class AvatarForm {

        @Constraints.Required
        public Integer x;

        @Constraints.Required
        public Integer y;

        @Constraints.Required
        public Integer width;

        @Constraints.Required
        public Integer height;

        public String validate() {
            if (!this.width.equals(this.height)) {
                return "The chosen extract is not rectangular";
            }
            return null;
        }

    }
}