package models;

import models.base.BaseNotifiable;
import models.base.INotifiable;
import org.hibernate.annotations.Type;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.List;

/**
 * Model for a chat message.
 */
@Entity
@Table
public class ChatMessage extends BaseNotifiable implements INotifiable {
    @ManyToOne
    public Account sender;

    @ManyToOne
    public Account recipient;

    @Type(type = "org.hibernate.type.TextType")
    public String message;

    /**
     * True, if this chat message is transmitted to its recipient.
     */
    @Column(name = "is_transmitted", nullable = false, columnDefinition = "boolean default false")
    public boolean isTransmitted;

    /**
     * Default constructor.
     */
    public ChatMessage() {
    }

    /**
     * Constructor with arguments.
     *
     * @param sender Sender
     * @param recipient Recipient
     * @param message message
     */
    public ChatMessage(Account sender, Account recipient, String message) {
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.isTransmitted = false;
    }

    @Override
    public void create() {
        JPA.em().persist(this);
    }

    @Override
    public void update() {
        this.updatedAt();
    }

    @Override
    public void delete() {
        JPA.em().remove(this);
    }

    @Override
    public Account getSender() {
        return null;
    }

    @Override
    public List<Account> getRecipients() {
        return null;
    }
}
