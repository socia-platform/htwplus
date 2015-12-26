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