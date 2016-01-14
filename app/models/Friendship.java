package models;

import models.base.BaseNotifiable;
import models.base.INotifiable;
import models.enums.LinkType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {"account_id", "friend_id"}))
public class Friendship extends BaseNotifiable implements INotifiable {
    public static final String FRIEND_REQUEST_SUCCESS = "request_successful";
    public static final String FRIEND_REQUEST_DECLINE = "request_decline";
    public static final String FRIEND_NEW_REQUEST = "new_request";
    public static final int PAGE = 1;

    @ManyToOne
    @NotNull
    public Account account;

    @ManyToOne
    @NotNull
    public Account friend;

    @Enumerated(EnumType.STRING)
    @NotNull
    public LinkType linkType;

    public Friendship() {
    }

    public Friendship(Account account, Account friend, LinkType type) {
        this.account = account;
        this.friend = friend;
        this.linkType = type;
    }

    @Override
    public Account getSender() {
        return this.type.equals(Friendship.FRIEND_REQUEST_DECLINE)
                ? this.friend
                : this.account;
    }

    @Override
    public List<Account> getRecipients() {
        return this.type.equals(Friendship.FRIEND_REQUEST_DECLINE)
                ? this.getAsAccountList(this.account)
                : this.getAsAccountList(this.friend);
    }

    @Override
    public String getTargetUrl() {
        if (this.type.equals(Friendship.FRIEND_NEW_REQUEST) || this.type.equals(Friendship.FRIEND_REQUEST_DECLINE)) {
            return controllers.routes.FriendshipController.index().toString();
        }

        if (this.type.equals(Friendship.FRIEND_REQUEST_SUCCESS)) {
            return controllers.routes.ProfileController.stream(this.account.id, Friendship.PAGE, false).toString();
        }

        return super.getTargetUrl();
    }
}
