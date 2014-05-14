package models;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import models.base.BaseModel;
import play.Play;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

@Entity
public class Media extends BaseModel {
	
	@Required
	public String title;
	
	@Required
	public String fileName;
	
	public String description;
	
	@Required
	public String url;

	@Required
	public String mimetype;
	
	@Required
	public Long size;
	
	@ManyToOne
	public Group group;
		
	@ManyToOne
	public Account owner;
	
	@Transient
	public File file;
	
	public static String GROUP = "group";
	
	public static Media findById(Long id) {
		Media media = JPA.em().find(Media.class, id);
		if(media == null) {
			return null;
		}
	    String path = Play.application().path().toString();
	    String relPath = Play.application().configuration().getString("media.relativePath");
		media.file = new File(path + "/" + relPath + "/" + media.url);
		if(media.file.exists()) {
			return media;
		} else {
			return null;
		}
	}
	
	public boolean existsInGroup(Group group) {
		List<Media> media = group.media;
		for (Media m : media) {
			if(m.title.equals(this.title)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void create() {
		this.size = file.length();
		this.url = this.createRelativeURL() + "/" + this.getUniqueFileName(this.fileName);
		try {
			this.createFile();
			JPA.em().persist(this);
		} catch (Exception e) {
			try {
				throw e;
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	@Override
	public void update() {
		JPA.em().merge(this);
	}
	
	@Override
	public void delete() {
		try {
			this.deleteFile();
			JPA.em().remove(this);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private String getUniqueFileName(String fileName) {
		return UUID.randomUUID().toString() + '_' + fileName;
	}
	
	private void deleteFile() throws FileNotFoundException{
		String path = Play.application().path().toString();
		String relPath = Play.application().configuration().getString("media.relativePath");
		File file = new File(path + "/" + relPath + "/" + this.url);
		if (file.exists()) {
			file.delete();
		} else {
			throw new FileNotFoundException("File does not exist.");
		}
	}
	
	private void createFile() throws Exception {
	    String path = Play.application().path().toString();
	    String relPath = Play.application().configuration().getString("media.relativePath");
	    File newFile = new File(path + "/" + relPath + "/" + this.url);
	    if(newFile.exists()){
	    	throw new Exception("File exists already");
	    }
	    newFile.getParentFile().mkdirs();
	    this.file.renameTo(newFile);
	    if(!newFile.exists()) {
	    	throw new Exception("Could not upload file");
	    }
	}
	
	private String createRelativeURL() {
		Date now = new Date();
		String format = new SimpleDateFormat("yyyy/MM/dd").format(now);
		return format;
	}

	public static boolean isOwner(Long mediaId, Account account) {
		Media m = JPA.em().find(Media.class, mediaId);
		if(m.owner.equals(account)){
			return true;
		}else {
			return false;
		}
	}
	
	public static int byteAsMB(long size) {
		return (int)(size / 1024 / 1024);
	}
	
	public boolean belongsToGroup(){
		if(this.group != null) return true;
		return false;
	}
		
}