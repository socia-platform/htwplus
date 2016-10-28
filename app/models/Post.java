package models;

import managers.NotificationManager;
import models.base.BaseModel;
import models.base.BaseNotifiable;
import models.base.INotifiable;
import org.hibernate.annotations.Type;
import play.Logger;
import play.data.validation.Constraints.Required;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
public class Post extends BaseNotifiable implements INotifiable {
    public static final String GROUP = "group";                             // post to group news stream
    public static final String PROFILE = "profile";                         // post to own news stream
    public static final String STREAM = "stream";                           // post to a foreign news stream
    public static final String COMMENT_PROFILE = "comment_profile";         // comment on a profile post
    public static final String COMMENT_GROUP = "comment_group";             // comment on a group post
    public static final String COMMENT_OWN_PROFILE = "comment_profile_own"; // comment on own news stream
    public static final String BROADCAST = "broadcast";                     // broadcast post from admin control center

    @Required
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    public String content;

    @ManyToOne
    public Post parent;

    @ManyToOne
    public Group group;

    @ManyToOne
    public Account account;

    @ManyToOne
    public Account owner;

    @Column(name = "is_broadcast", nullable = false, columnDefinition = "boolean default false")
    public boolean isBroadcastMessage;

    @ManyToMany
    @JoinTable(
            name = "broadcast_account",
            joinColumns = {@JoinColumn(name = "post_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "account_id", referencedColumnName = "id")}
    )
    public List<Account> broadcastPostRecipients;

    @Transient
    public String searchContent;

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE)
    public Set<PostBookmark> postBookmarks;

    public String validate() {
        if (this.content.trim().length() <= 0) {
            return "Empty post!";
        }
        return null;
    }


    @Override
    public Account getSender() {
        return this.owner;
    }

    @Override
    public List<Account> getRecipients() {
        if (this.type.equals(Post.BROADCAST)) {
            return this.broadcastPostRecipients;
        }

        // if this is a comment, return the parent information
        if (this.parent != null) {
            // if this is a comment on own news stream, send notification to the initial poster
            if (this.type.equals(Post.COMMENT_OWN_PROFILE)) {
                return this.parent.getAsAccountList(this.parent.owner);
            }

            return this.parent.belongsToAccount()
                    ? this.parent.getAsAccountList(this.parent.account)
                    : this.parent.getGroupAsAccountList(this.parent.group);
        }

        // return account if not null, otherwise group
        return this.belongsToAccount()
                ? this.getAsAccountList(this.account)
                : this.getGroupAsAccountList(this.group);
    }

    /**
     * Adds an account to the persistent recipient list.
     *
     * @param recipient One of the recipients
     */
    public void addRecipient(Account recipient) {
        if (this.broadcastPostRecipients == null) {
            this.broadcastPostRecipients = new ArrayList<>();
        }

        if (!this.broadcastPostRecipients.contains(recipient)) {
            this.broadcastPostRecipients.add(recipient);
        }
    }

    @Override
    public String getTargetUrl() {
        if (this.type.equals(Post.GROUP)) {
            return controllers.routes.PostController.view(this.id).toString();
        }

        if (this.type.equals(Post.PROFILE)) {
            return controllers.routes.PostController.view(this.id).toString();
        }

        if (this.type.equals(Post.COMMENT_PROFILE)) {
            return controllers.routes.PostController.view(this.parent.id).toString();
        }

        if (this.type.equals(Post.COMMENT_GROUP)) {
            return controllers.routes.PostController.view(this.parent.id).toString();
        }

        if (this.type.equals(Post.COMMENT_OWN_PROFILE)) {
            return controllers.routes.PostController.view(this.parent.id).toString();
        }

        if (this.type.equals(Post.BROADCAST)) {
            return controllers.routes.PostController.view(this.id).toString();
        }

        return super.getTargetUrl();
    }

    /**
     * As the notifications should only refer to the main post, we need to return the parent, if given.
     * Otherwise this is the main post and we can return this.
     *
     * @return Post instance
     */
    @Override
    public BaseModel getReference() {
        if (this.parent != null) {
            return this.parent;
        }

        return this;
    }

    /**
     * As we want to have only one notification per post and just update if there is a new comment,
     * we need to find out, if there is a notification per post and user already given. If there is no
     * notification given for a user and post, we create a new notification instance.
     *
     * @param recipient Account recipient
     * @return Notification instance
     */
    @Override
    public Notification getNotification(Account recipient) {
        if (this.parent != null) {
            try {
                return NotificationManager.findByReferenceIdAndRecipientId(this.parent.id, recipient.id);
            } catch (NoResultException ex) {
                Logger.error("Error while trying to fetch notification for Post ID: " + this.parent.id
                                + ", Recipient ID: " + recipient.id + ": " + ex.getMessage()
                );
            }
        }

        return new Notification();
    }

    public boolean belongsToAccount() {
        return this.account != null;
    }

    public boolean belongsToGroup() {
        return this.group != null;
    }

    public boolean belongsToPost() {
        return this.parent != null;
    }
}