package models;

import models.base.BaseModel;
import models.enums.LinkType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "group_account", uniqueConstraints = @UniqueConstraint(columnNames = {
        "account_id", "group_id"}))
public class GroupAccount extends BaseModel {

    @ManyToOne(optional = false)
    public Group group;

    @ManyToOne(optional = false)
    public Account account;

    @Enumerated(EnumType.STRING)
    @NotNull
    public LinkType linkType;

    public GroupAccount() {
    }

    public GroupAccount(final Group group, final Account account, final LinkType linkType) {
        this.group = group;
        this.account = account;
        this.linkType = linkType;
    }

}
