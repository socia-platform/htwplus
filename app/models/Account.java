package models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import managers.AvatarManager;
import models.base.BaseModel;
import models.base.IJsonNodeSerializable;
import models.enums.AccountRole;
import models.enums.EmailNotifications;
import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.URL;
import play.Logger;
import play.data.validation.Constraints;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.libs.Json;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.Set;

@Entity
public class Account extends BaseModel implements IJsonNodeSerializable {

    public String loginname;

    public String name;

    @Required
    public String firstname;

    @Required
    public String lastname;

    @Email
    @Column(unique = true)
    public String email;

    @Required
    public String password;

    public String avatar;

    @Type(type = "org.hibernate.type.TextType")
    public String about;

    @URL(message = "error.homepage")
    public String homepage;

    @OneToMany(mappedBy = "account", orphanRemoval = true)
    public Set<Friendship> friends;

    @OneToOne
    public Studycourse studycourse;
    public String degree;
    public Integer semester;

    public AccountRole role;

    public EmailNotifications emailNotifications;

    public Integer dailyEmailNotificationHour;

    /**
     * Returns the initials of the user as an alternative to the avatar
     *
     * @return
     */
    public String getInitials() {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(this.firstname.charAt(0)));
        sb.append(Character.toUpperCase(this.lastname.charAt(0)));
        return sb.toString();
    }

    @Override
    public ObjectNode getAsJson() {
        ObjectNode node = Json.newObject();
        node.put("id", this.id);
        node.put("name", this.name);

        return node;
    }

    /**
     * Determines if the user has a custom avatar
     *
     * @return
     */
    public boolean hasAvatar() {
        if (this.avatar.equals(AvatarManager.AVATAR_CUSTOM)) {
            return true;
        }
        return false;
    }

    static public class AvatarForm {

        @Constraints.Required
        public Integer x;

        @Constraints.Required
        public Integer y;

        @Constraints.Required
        public Integer width;

        @Constraints.Required
        public Integer height;

        public String validate() {
            if (!this.width.equals(this.height)) {
                return "The chosen extract is not rectangular";
            }
            return null;
        }

    }
}