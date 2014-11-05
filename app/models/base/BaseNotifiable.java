package models.base;


import models.Account;
import models.Group;
import models.GroupAccount;
import models.Notification;
import models.enums.LinkType;
import models.services.TemplateService;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class as extension to BaseModel for classes, that are notifiable.
 */
public abstract class BaseNotifiable extends BaseModel implements INotifiable {
    /**
     * Type of this notifiable instance. This might be usable, if the render() method, needs to differentiate
     * between more than one possible states of this notifiable object (e.g. a post can be profile, stream
     * or group post).
     */
    @Transient
    public String type;

    /**
     * This list can be used as a temporary recipient list, if the notifiable object does not save the recipient list
     * in the model.
     */
    @Transient
    public List<Account> temporaryRecipients;

    /**
     * This attribute can be used as a temporary sender, if the notifiable object does not save the sender in the model.
     */
    @Transient
    public Account temporarySender;

    @Override
    public BaseModel getReference() {
        return this;
    }

    /**
     * Returns the fully qualified path to the compiled template class desired for this notification as string.
     * Example:
     * - For post:  There are types like PROFILE or STREAM posts, therefore the fully qualified
     *              string for a profile post is: views.html.Notification.post.profile
     *
     * @return The desired template class as string
     */
    protected String getTemplateClass() {
        return this.type.equals("")
            ? "views.html.Notification." + this.getClass().getSimpleName().toLowerCase()
            : "views.html.Notification." + this.getClass().getSimpleName().toLowerCase() + "."
                + this.type.toLowerCase();
    }

    @Override
    public String render(Notification notification) {
        return TemplateService.getInstance().getRenderedTemplate(this.getTemplateClass(), notification, this);
    }

    @Override
    public String getTargetUrl() {
        return controllers.routes.Application.index().toString();
    }

    @Override
    public Notification getNotification(Account recipient) {
        return new Notification();
    }

    /**
     * Helper method to return one account as a List<Account> as required by getRecipients()
     * in the INotifiable interface.
     *
     * @param account One account to be returned as list
     * @return List of account instances
     */
    public List<Account> getAsAccountList(Account account) {
        List<Account> accounts = new ArrayList<>();
        accounts.add(account);

        return accounts;
    }

    /**
     * Returns all accounts that are assigned to a group as list.
     *
     * @param group Group to retrieve a list of accounts from
     * @return List of accounts of group
     */
    public List<Account> getGroupAsAccountList(final Group group) {
    	return GroupAccount.findAccountsByGroup(group, LinkType.establish);
    }

    /**
     * Adds an account to the temporary recipient list.
     *
     * @param recipient One of the temporary recipients
     */
    public void addTemporaryRecipient(Account recipient) {
        if (this.temporaryRecipients == null) {
            this.temporaryRecipients = new ArrayList<>();
        }

        if (!this.temporaryRecipients.contains(recipient)) {
            this.temporaryRecipients.add(recipient);
        }
    }
}
