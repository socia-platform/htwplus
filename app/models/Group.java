package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import models.base.BaseNotifiable;
import models.base.INotifiable;
import models.enums.GroupType;
import models.enums.LinkType;

import models.services.ElasticsearchService;
import play.data.validation.ValidationError;
import play.data.validation.Constraints.Required;
import play.data.validation.Constraints.Pattern;
import play.db.jpa.JPA;

@Entity
@Table(name = "Group_")
public class Group extends BaseNotifiable implements INotifiable {
    public static final String GROUP_INVITATION = "group_invitation";
    public static final String GROUP_NEW_REQUEST = "group_new_request";
    public static final String GROUP_REQUEST_SUCCESS = "group_request_success";
    public static final String GROUP_REQUEST_DECLINE = "group_request_decline";

    @Required
	@Column(unique = true)
    @Pattern(value="^[ A-Za-z0-9\u00C0-\u00FF.!#$%&'+=?_{|}/\\\\\\[\\]~-]+$")
	public String title;

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
		if (Group.findByTitle(this.title) != null) {
			errors.add(new ValidationError("title", "error.title"));
			return errors;
		}
		return null;
	}

	public static boolean validateToken(String token) {
        return !(token.equals("") || token.length() < 4 || token.length() > 45);
	}

	public void createWithGroupAccount(Account account) {
		this.owner = account;
		this.create();
		GroupAccount groupAccount = new GroupAccount(account, this,
				LinkType.establish);
		groupAccount.create();

	}

	@Override
	public void create() {
        JPA.em().persist(this);
         try {
             ElasticsearchService.indexGroup(this);
         } catch (IOException e) {
             e.printStackTrace();
         }
    }

	@Override
	public void update() {
         try {
             ElasticsearchService.indexGroup(this);
         } catch (IOException e) {
             e.printStackTrace();
         }
		JPA.em().merge(this);
	}

	@Override
	public void delete() {
		// delete all Posts
		List<Post> posts = Post.getPostsForGroup(this, 0, 0);
		for (Post post : posts) {
			post.delete();
		}

		// delete media
		for (Media media : this.media) {
			media.delete();
		}

		// Delete Notifications
        Notification.deleteReferences(this);

        // Delete Elasticsearch document
         ElasticsearchService.deleteGroup(this);

		JPA.em().remove(this);
	}

	public static Group findById(Long id) {
		return JPA.em().find(Group.class, id);
	}

	public static Group findByTitle(String title) {
		@SuppressWarnings("unchecked")
		List<Group> groups = (List<Group>) JPA.em()
				.createQuery("FROM Group g WHERE g.title = ?1")
				.setParameter(1, title).getResultList();

		if (groups.isEmpty()) {
			return null;
		} else {
			return groups.get(0);
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Group> all() {
        return JPA.em().createQuery("FROM Group").getResultList();
	}

    /**
     * Returns true, if an account is member of a group.
     *
     * @param group Group instance
     * @param account Account instance
     * @return True, if account is member of group
     */
	public static boolean isMember(Group group, Account account) {
		@SuppressWarnings("unchecked")
		List<GroupAccount> groupAccounts = (List<GroupAccount>) JPA
				.em()
				.createQuery(
						"SELECT g FROM GroupAccount g WHERE g.account.id = ?1 and g.group.id = ?2 AND linkType = ?3")
				.setParameter(1, account.id).setParameter(2, group.id)
				.setParameter(3, LinkType.establish).getResultList();

        return !groupAccounts.isEmpty();
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
            return controllers.routes.GroupController.stream(this.id, 1).toString();
        }

        return controllers.routes.GroupController.index().toString();
    }

    public static long indexAllGroups() throws IOException {
        final long start = System.currentTimeMillis();
        for (Group group: all()) ElasticsearchService.indexGroup(group);
        return (System.currentTimeMillis() - start) / 100;

    }
}
