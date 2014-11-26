package mock;

import models.Account;
import models.Notification;
import models.base.BaseModel;
import models.base.INotifiable;

import java.util.List;

/**
 * Mock for notifiable interface.
 */
public class MockNotifiable implements INotifiable {
    public Account sender;
    public List<Account> recipients;
    protected BaseModel reference = new MockBaseModel();
    public String rendered = "[MOCK-UP]";

    @Override
    public Account getSender() {
        return this.sender;
    }

    @Override
    public List<Account> getRecipients() {
        return this.recipients;
    }

    @Override
    public String render(Notification notification) {
        return this.rendered;
    }

    @Override
    public BaseModel getReference() {
        return this.reference;
    }

    @Override
    public String getTargetUrl() {
        return "[MOCK-UP]";
    }

    @Override
    public Notification getNotification(Account recipient) {
        return new Notification();
    }
}
