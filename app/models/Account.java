package models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Set;

import java.io.File;

import javax.persistence.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import models.base.BaseModel;
import models.base.FileOperationException;
import models.base.IJsonNodeSerializable;
import models.enums.AccountRole;
import models.enums.EmailNotifications;
import models.services.AvatarService;
import models.services.FileService;
import models.base.ValidationException;

import models.services.ElasticsearchService;
import play.Logger;
import play.data.validation.Constraints;
import play.mvc.Http.MultipartFormData;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import controllers.Component;
import play.libs.Json;

@Entity
public class Account extends BaseModel implements IJsonNodeSerializable {

	private final static Logger.ALogger logger = Logger.of(Account.class);

	public String loginname;

	public String name;

	@Required
	public String firstname;

	@Required
	public String lastname;

	@Email
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
	public static Account findById(Long id) {
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
        try {
            ElasticsearchService.indexAccount(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
	
	public String getAvatarUrl() {
		String url = controllers.routes.Assets.at("images/avatars/" + this.avatar + ".png").toString();
		return url;
	}

	public void setTempAvatar(MultipartFormData.FilePart filePart) throws ValidationException {
		FileService fileService = new FileService("tempavatar");
		fileService.setFilePart(filePart);
		if(!fileService.validateSize(FileService.MBAsByte(20))) {
			throw new ValidationException("File to big.");
		}
		String[] allowedContentTypes = { FileService.MIME_JPEG };
		if(!fileService.validateContentType(allowedContentTypes)) {
			throw new ValidationException("Content Type is not supported.");
		}
		if(!AvatarService.validateSize(fileService.getFile())){
			throw new ValidationException("Image Resolution is to little.");
		}

		fileService.saveFile(this.getTempAvatarName(), true);
	}
	
	public File getTempAvatar() {
		FileService fileService = new FileService("tempavatar");
		return fileService.openFile(this.getTempAvatarName());
	}
	
	public void saveAvatar(AvatarForm avatarForm){
		FileService fileService = new FileService("tempavatar");
		File avatarFile = fileService.copyFile(this.getTempAvatarName(), this.getAvatarName());
		try {
			AvatarService.crop(avatarFile, avatarForm.x, avatarForm.y, avatarForm.width, avatarForm.height);
			File thumbFile = fileService.copyFile(this.getAvatarName(), this.getThumbName());
			AvatarService.resizeToAvatar(avatarFile);
			AvatarService.resizeToThumbnail(thumbFile);
		} catch (FileOperationException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public File getAvatar(boolean thumb) {
		FileService fileService = new FileService("tempavatar");
		if(!thumb) {
			return fileService.openFile(this.getAvatarName());
		} else {
			return fileService.openFile(this.getThumbName());
		}
	}
	
	private String getAvatarName(){
		String fileName = this.id.toString() + "_avatar.jpg";
		return fileName;
	}

	private String getThumbName(){
		String fileName = this.id.toString() + "_thumb.jpg";
		return fileName;
	}
	
	private String getTempAvatarName(){
		String fileName = this.id.toString() + ".jpg";
		return fileName;
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

    public static long indexAllAccounts() throws IOException {
        final long start = System.currentTimeMillis();
        for (Account account: all()) ElasticsearchService.indexAccount(account);
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
		
		// ToDo Validate Square Geo

	}

}