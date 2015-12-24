package models;

import models.base.BaseNotifiable;
import models.base.INotifiable;
import models.enums.LinkType;
import play.Play;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.io.File;
import java.util.List;

@Entity
public class Media extends BaseNotifiable implements INotifiable {
    public static final String MEDIA_NEW_MEDIA = "media_new_media";

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

    @Transient
    public String sizeInByte;

    public static String GROUP = "group";

    public static Media findById(Long id) {
        Media media = JPA.em().find(Media.class, id);
        if (media == null) {
            return null;
        }
        String path = Play.application().configuration().getString("media.path");
        media.file = new File(path + "/" + media.url);
        if (media.file.exists()) {
            return media;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Media> listAllOwnedBy(Long id) {
        return JPA.em().createQuery("FROM Media m WHERE m.owner.id = " + id).getResultList();
    }

    public boolean existsInGroup(Group group) {
        List<Media> media = group.media;
        for (Media m : media) {
            if (m.title.equals(this.title)) {
                return true;
            }
        }
        return false;
    }


    public static boolean isOwner(Long mediaId, Account account) {
        Media m = JPA.em().find(Media.class, mediaId);
        if (m.owner.equals(account)) {
            return true;
        } else {
            return false;
        }
    }

    public static int byteAsMB(long size) {
        return (int) (size / 1024 / 1024);
    }

    public boolean belongsToGroup() {
        if (this.group != null) return true;
        return false;
    }

    @Override
    public Account getSender() {
        return this.temporarySender;
    }

    @Override
    public List<Account> getRecipients() {
        // new media available in group, whole group must be notified
        return groupAccountManager.findAccountsByGroup(this.group, LinkType.establish);
    }

    @Override
    public String getTargetUrl() {
        return controllers.routes.GroupController.media(this.group.id).toString();
    }
}