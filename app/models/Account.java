package models;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import java.io.File;

import javax.persistence.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.typesafe.config.ConfigFactory;
import models.base.BaseModel;
import models.base.FileOperationException;
import models.base.IJsonNodeSerializable;
import models.enums.AccountRole;
import models.enums.EmailNotifications;
import models.services.AvatarService;
import models.services.FileService;
import models.base.ValidationException;

import play.Logger;
import play.data.validation.Constraints;
import play.i18n.Messages;
import play.mvc.Http.MultipartFormData;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import controllers.Component;
import play.libs.Json;
import util.ExposeField;

@Entity
public class Account extends BaseModel implements IJsonNodeSerializable {

	private final static Logger.ALogger logger = Logger.of(Account.class);

	public String loginname;

	public String name;

	@Required
	@ExposeField(name ="firstname", template = "John")
	public String firstname;

	@Required
	@ExposeField(name = "lastname", template = "Doe")
	public String lastname;

	@Email
	@ExposeField(name = "email", template = "john@doe-online.com")
	@Column(unique=true)
	public String email;

	@Required
	public String password;

	public String avatar;

	@OneToMany(mappedBy = "account")
	public Set<Friendship> friends;

	@OneToMany(mappedBy="account")
	public Set<GroupAccount> groupMemberships;

	public Date lastLogin;

	public String studentId;

	@OneToOne
	@ExposeField(name = "studycourse")
	public Studycourse studycourse;
	public String degree;
	public Integer semester;

	public AccountRole role;

    public EmailNotifications emailNotifications;

    public Integer dailyEmailNotificationHour;

	public Boolean approved;

    /**
     * Returns an account by account ID.
     *
     * @param id Account ID
     * @return Account instance
     */
	public static Account findById(long id) {
		return JPA.em().find(Account.class, id);
	}

    @SuppressWarnings("unchecked")
	public static List<Account> findAll(){
		return JPA.em().createQuery("SELECT a FROM Account a ORDER BY a.name").getResultList();
	}

	@Override
	public void create() {
		this.name = firstname+" "+lastname;
		JPA.em().persist(this);
        // try {
        //     ElasticsearchService.indexAccount(this);
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
    }

	@Override
	public void update() throws PersistenceException {
		this.name = this.firstname+" "+this.lastname;
		JPA.em().merge(this);
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
	}

	/**
     * Retrieve a User from email.
     */
    public static Account findByEmail(String email) {
    	if(email.isEmpty()) {
    		return null;
    	}
    	try{
	    	return (Account) JPA.em()
					.createQuery("from Account a where a.email = :email")
					.setParameter("email", email).getSingleResult();
	    } catch (NoResultException exp) {
	    	return null;
		}
    }

	/**
     * Retrieve a User by loginname
     */
    public static Account findByLoginName(String loginName) {
    	try{
	    	return (Account) JPA.em()
					.createQuery("from Account a where a.loginname = :loginname")
					.setParameter("loginname", loginName).getSingleResult();
	    } catch (NoResultException exp) {
	    	return null;
		}
    }

    /**
     * Retrieve a User by loginname
     */
    public static Account findByName(String name) {
        try{
            return (Account) JPA.em()
                    .createQuery("from Account a where a.name = :name")
                    .setParameter("name", name).getSingleResult();
        } catch (NoResultException exp) {
            return null;
        }
    }

    /**
     * Authenticates a user by email and password.
     * @param email of the user who wants to be authenticate
     * @param password of the user should match to the email ;)
     * @return Returns the current account or Null
     */
	public static Account authenticate(String email, String password) {
		Account currentAcc = null;
		try {
			final Account result = (Account) JPA.em()
				.createQuery("from Account a where a.email = :email")
				.setParameter("email", email).getSingleResult();
			if (result != null && Component.md5(password).equals(result.password)) {
				currentAcc = result;
			}
			return currentAcc;
		} catch (NoResultException exp) {
			return currentAcc;
		}
	}

	static public String AVATAR_REALM = "avatar";
	static public int AVATAR_MIN_SIZE = 250;
	static public int AVATAR_MAX_SIZE = 4000;
	static public int AVATAR_LARGE_SIZE = 600;
	static public int AVATAR_MEDIUM_SIZE = 140;
	static public int AVATAR_SMALL_SIZE = 70;
	static public String AVATAR_CUSTOM = "custom";
	public enum AVATAR_SIZE {
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
		if(!fileService.validateSize(FileService.MBAsByte(maxSize))) {
			throw new ValidationException(Messages.get("error.fileToBig"));
		}
		String[] allowedContentTypes = { FileService.MIME_JPEG, FileService.MIME_PNG };
		if(!fileService.validateContentType(allowedContentTypes)) {
			throw new ValidationException(Messages.get("error.contentTypeNotSupported"));
		}
		if(!AvatarService.validateMinSize(fileService.getFile(), Account.AVATAR_MIN_SIZE, Account.AVATAR_MIN_SIZE)){
			throw new ValidationException(Messages.get("error.resolutionLow"));
		}
		if(!AvatarService.validateMaxSize(fileService.getFile(), Account.AVATAR_MAX_SIZE, Account.AVATAR_MAX_SIZE)){
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
	private String getTempAvatarName(){
		String fileName = this.id.toString() + ".jpg";
		return fileName;
	}

	/**
	 * Determines if the user has a custom avatar
	 *
	 * @return
	 */
	public boolean hasAvatar(){
		if(this.avatar.equals(AVATAR_CUSTOM)){
			return true;
		}
		return false;
	}

	/**
	 * Returns the initials of the user as an alternative to the avatar
	 *
	 * @return
	 */
	public String getInitials(){
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toUpperCase(this.firstname.charAt(0)));
		sb.append(Character.toUpperCase(this.lastname.charAt(0)));
		return sb.toString();
	}

	public static boolean isOwner(Long accountId, Account currentUser) {
		Account a = JPA.em().find(Account.class, accountId);
		if(a.equals(currentUser)){
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Try to get all accounts...
	 * @return List of accounts.
	 */
	@SuppressWarnings("unchecked")
	public static List<Account> all() {
        return JPA.em().createQuery("FROM Account").getResultList();
	}

	/**
	 * Try to get a chunk of accounts
	 * @param offset First x accounts will be skipped
	 * @param count Get only x accounts
	 * @return
	 */
	public static List<Account> some(int offset, int count) {
		return JPA.em().createQuery("From Account")
				.setFirstResult(offset)
				.setMaxResults(count)
				.getResultList();
	}


    /**
     * Returns a list of account instances by an ID collection of Strings.
     *
     * @param accountIds String array of account IDs
     * @return List of accounts
     */
    public static List<Account> getAccountListByIdCollection(final List<String> accountIds) {
    	StringBuilder joinedAccountIds = new StringBuilder();
        for (int i = 0; i < accountIds.size(); i++) {
            if (i > 0) {
                joinedAccountIds.append(",");
            }
            joinedAccountIds.append(accountIds.get(i));
        }

        return JPA.em()
                .createQuery("FROM Account a WHERE a.id IN (" +joinedAccountIds.toString() + ")", Account.class)
                .getResultList();
    }

    @Override
    public ObjectNode getAsJson() {
        ObjectNode node = Json.newObject();
        node.put("id", this.id);
        node.put("name", this.name);

        return node;
    }

	@SuppressWarnings("unchecked")
    public static List<Account> getAllNames(){
        return JPA.em().createQuery("SELECT a.id, a.name FROM Account a").getResultList();
    }

    /**
     * Index the current account
     */
    public void indexAccount() {
        // try {
        //     ElasticsearchService.indexAccount(this);
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
    }

    /**
     * Index all accounts
     */
    public static long indexAllAccounts() throws IOException {
        final long start = System.currentTimeMillis();
        // for (Account account: all()) ElasticsearchService.indexAccount(account);
        return (System.currentTimeMillis() - start) / 100;

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
			if(!this.width.equals(this.height)) {
				return "The chosen extract is not rectangular";
			}
			return null;
		}

	}
}