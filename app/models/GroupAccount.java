package models;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import play.db.jpa.*;

import java.util.List;

import models.base.BaseModel;
import models.enums.GroupType;
import models.enums.LinkType;
import play.libs.F;

@Entity
@Table(name = "group_account", uniqueConstraints = @UniqueConstraint(columnNames = {
		"account_id", "group_id" }))
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

	public GroupAccount(Account account, Group group, LinkType linkType) {
		this.account = account;
		this.group = group;
		this.linkType = linkType;
	}

	public static GroupAccount findById(Long id) {
		return JPA.em().find(GroupAccount.class, id);
	}

	@Override
	public void create() {
		JPA.em().persist(this);
	}

	@Override
	public void update() {
		JPA.em().merge(this);
	}

	@Override
	public void delete() {
        Notification.deleteReferencesForAccountId(this.group, this.account.id);
		JPA.em().remove(this);
	}
	
	/**
	 * Find all groups and courses where given account is owner or member
	 */
	public static List<Group> findEstablished(Account account) {
		@SuppressWarnings("unchecked")
		List<Group> groupAccounts = JPA
				.em()
				.createQuery(
						"SELECT ga.group FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.linkType = ?2")
				.setParameter(1, account.id)
				.setParameter(2, LinkType.establish).getResultList();
		return groupAccounts;
	}

	/**
	 * Find all groups where given account is owner or member
	 */
	public static List<Group> findGroupsEstablished(Account account) {
		@SuppressWarnings("unchecked")
		List<Group> groupAccounts = JPA
				.em()
				.createQuery(
						"SELECT ga.group FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.linkType = ?2 AND ga.group.groupType != ?3 ORDER BY ga.group.title ASC")
				.setParameter(1, account.id)
				.setParameter(2, LinkType.establish)
				.setParameter(3, GroupType.course).getResultList();
		return groupAccounts;
	}
	
	/**
	 * Find all courses where given account is owner or member.
	 */
	public static List<Group> findCoursesEstablished(final Account account)
	{
		@SuppressWarnings("unchecked")
		List<Group> courseAccounts = JPA
				.em()
				.createQuery(
						"SELECT ga.group FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.linkType = ?2 AND ga.group.groupType = ?3  ORDER BY ga.group.title ASC")
				.setParameter(1, account.id)
				.setParameter(2, LinkType.establish)
				.setParameter(3, GroupType.course).getResultList();
		return courseAccounts;
	}
	/**
	 * Find all open groups where given account is owner or member 
	 */
    @SuppressWarnings("unchecked")
	public static List<Group> findPublicEstablished(final Account account) {
    	return JPA
                .em()
                .createQuery(
                        "SELECT ga.group FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.linkType = ?2 AND ga.group.groupType = ?3")
                .setParameter(1, account.id)
                .setParameter(2, LinkType.establish)
                .setParameter(3, GroupType.open).getResultList();
	}

	/**
	 * Find all requests and rejects for summarization under "Offene Anfragen"
	 * for given Account
	 * 
	 * @param account Account instance
	 * @return List of group accounts
	 */
	public static List<GroupAccount> findRequests(Account account) {
		@SuppressWarnings("unchecked")
		List<GroupAccount> groupAccounts = JPA
				.em()
				.createQuery(
						"SELECT ga FROM GroupAccount ga WHERE ((ga.group.owner.id = ?1 OR ga.account.id = ?1) AND ga.linkType = ?2) OR (ga.account.id = ?1 AND ga.linkType = ?3) OR (ga.account.id = ?1 AND ga.linkType = ?4)")
				.setParameter(1, account.id).setParameter(2, LinkType.request)
				.setParameter(3, LinkType.reject).setParameter(4, LinkType.invite).getResultList();
		return groupAccounts;
	}
	
	/**
	 * Has account any link-types to given group?
     *
	 * @param account Account instance
	 * @param group Group instance
	 * @return True, if an account has a link type for a group
	 */
	public static boolean hasLinkTypes(Account account, Group group) {
		try {
			JPA.em().createQuery("SELECT ga FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.group.id = ?2")
			.setParameter(1, account.id).setParameter(2, group.id).getSingleResult();
		} catch (NoResultException exp) {
	    	return false;
		}
		return true;
	}
	
	/**
	 * Retrieve Accounts from Group with given LinkType.
	 */
	public static List<Account> findAccountsByGroup(final Group group, final LinkType type) {
		@SuppressWarnings("unchecked")
        List<Account> accounts = (List<Account>) JPA
                .em()
                .createQuery(
                        "SELECT ga.account FROM GroupAccount ga WHERE ga.group.id = ?1 AND ga.linkType = ?2")
                .setParameter(1, group.id).setParameter(2, type)
                .getResultList();
        return accounts;
	}

    /**
     * Returns a group account by account and group.
     *
     * @param account Account instance
     * @param group Group instance
     * @return Group account instance
     */
	public static GroupAccount find(Account account, Group group) {
		try {
			return (GroupAccount) JPA
					.em()
					.createQuery(
							"SELECT ga FROM GroupAccount ga WHERE ga.account.id = ?1 AND ga.group.id = ?2")
					.setParameter(1, account.id).setParameter(2, group.id)
					.getSingleResult();
		} catch (NoResultException exp) {
			return null;
		}
	}

}
