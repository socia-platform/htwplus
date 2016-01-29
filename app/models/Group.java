package models;

import managers.GroupManager;
import models.base.BaseNotifiable;
import models.base.INotifiable;
import models.enums.GroupType;
import play.data.validation.Constraints.Pattern;
import play.data.validation.Constraints.Required;
import play.data.validation.ValidationError;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Group_")
public class Group extends BaseNotifiable implements INotifiable {
    public static final String GROUP_INVITATION = "group_invitation";
    public static final String GROUP_NEW_REQUEST = "group_new_request";
    public static final String GROUP_REQUEST_SUCCESS = "group_request_success";
    public static final String GROUP_REQUEST_DECLINE = "group_request_decline";

    @Required
    @Column(unique = true)
    @Pattern(value = "^[ A-Za-z0-9\u00C0-\u00FF.!#$%&'+=?_{|}/\\\\\\[\\]~-]+$")
    public String title;

    @Size(max = 255, message = "error.length")
    public String description;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    public Set<GroupAccount> groupAccounts;

    @ManyToOne
    public Account owner;

    @Enumerated(EnumType.STRING)
    public GroupType groupType;

    public String token;

    @OneToMany(mappedBy = "group")
    @OrderBy("createdAt DESC")
    public List<Media> media;

    @OneToMany(mappedBy = "group")
    public List<Folder> folders;

    @OneToOne
    public Folder mediaFolder;

    public void setTitle(String title) {
        this.title = title.trim();
    }

    /**
     * Possible invitation list
     */
    @Transient
    public Collection<String> inviteList = null;


    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if (new GroupManager().findByTitle(this.title) != null) {
            errors.add(new ValidationError("title", "error.title"));
            return errors;
        }
        return null;
    }

    public static boolean validateToken(String token) {
        return !(token.equals("") || token.length() < 4 || token.length() > 45);
    }

    @Override
    public Account getSender() {
        return this.temporarySender;
    }

    @Override
    public List<Account> getRecipients() {
        switch (this.type) {
            case Group.GROUP_NEW_REQUEST:
                // group entry request notification, notify the owner of the group
                return this.getAsAccountList(this.owner);
        }

        // this is an invitation, a request accept or decline notification, notify the temporaryRecipients
        return this.temporaryRecipients;
    }

    @Override
    public String getTargetUrl() {
        if (this.type.equals(Group.GROUP_REQUEST_SUCCESS)) {
            return controllers.routes.GroupController.stream(this.id, 1, false).toString();
        }

        return controllers.routes.GroupController.index().toString();
    }
}