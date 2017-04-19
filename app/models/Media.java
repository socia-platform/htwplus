package models;

import managers.FolderManager;
import managers.GroupAccountManager;
import models.base.BaseNotifiable;
import models.base.INotifiable;
import models.enums.LinkType;
import play.data.validation.Constraints.Required;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.io.File;
import java.util.ArrayList;
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
    public Account owner;

    @Transient
    public File file;

    @ManyToOne
    public Folder folder;

    @Transient
    public String sizeInByte;

    public Folder findRoot() {
        return this.folder.findRoot(this.folder);
    }

    public Group findGroup() {
        return this.folder.findRoot(this.folder).group;
    }

    public Account findAccount() {
        return this.folder.findRoot(this.folder).account;
    }

    @Override
    public Account getSender() {
        return this.temporarySender;
    }

    @Override
    public List<Account> getRecipients() {
        // new media available in group, whole group must be notified (rootFolder knows the group)
        Folder rootFolder = FolderManager.findRoot(folder);
        List<Account> accounts = new ArrayList<>();
        if (rootFolder.group != null) {
            for (GroupAccount groupAccount : rootFolder.group.groupAccounts) {
                if (groupAccount.linkType.equals(LinkType.establish)) {
                    accounts.add(groupAccount.account);
                }
            }
        }
        return accounts;
    }

    @Override
    public String getTargetUrl() {
        return controllers.routes.GroupController.media(this.findGroup().id, folder.id).toString();
    }
}