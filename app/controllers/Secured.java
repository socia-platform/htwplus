package controllers;


import java.util.Date;

import models.*;
import models.enums.AccountRole;
import play.Logger;
import play.Play;
import play.i18n.Messages;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;
import views.html.landingpage;

/**
 * This class provides several authorization methods for security reasons.
 */
public class Secured extends Security.Authenticator {
	/**
	 * Returns the ID of the currently logged in user.
	 *
	 * @param ctx HTTP context
	 * @return ID or null
	 */
	@Override
    public String getUsername(Context ctx) {
		// see if user is logged in
        if (ctx.session().get("id") == null)
            return null;
 
        // see if the session is expired
        String previousTick = ctx.session().get("userTime");
        if (previousTick != null && !previousTick.equals("")) {
            long previousT = Long.valueOf(previousTick);
            long currentT = new Date().getTime();
            long timeout = Long.valueOf(Play.application().configuration().getString("sessionTimeout")) * 1000 * 60;
            long passedT = currentT - previousT;
            if (passedT > timeout && !ctx.session().containsKey("rememberMe")) {
                // session expired
            	ctx.session().clear();
            	play.mvc.Controller.flash("info", Messages.get("error.sessionExpired"));
                return null;
            } 
        }
 
        // update time in session
        String tickString = Long.toString(new Date().getTime());
        ctx.session().put("userTime", tickString);
		return ctx.session().get("id");
    }

	/**
	 * Returns Result instance to landing page (on un-authorization).
	 * @param ctx HTTP context
	 * @return Result instance
	 */
	@Override
    public Result onUnauthorized(Context ctx) {
		Logger.info("Unauthorized - Redirect to Login");
		return ok(landingpage.render());
    }

	/**
	 * Returns true, if the currently logged in user is admin.
	 *
	 * @return True, if admin
	 */
	public static boolean isAdmin() {
		Account current = Component.currentAccount();
		return current.role == AccountRole.ADMIN;
	}

	/**
	 * Returns true, if an account is member of a group.
	 *
	 * @param group Group
	 * @param account Account
	 * @return True, if account is member
	 */
	public static boolean isMemberOfGroup(Group group, Account account){
		return Group.isMember(group, account);
	}

	/**
	 * Returns true, if an account is owner of a group.
	 *
	 * @param group Group
	 * @param account Account
	 * @return True, if account is owner of group
	 */
	public static boolean isOwnerOfGroup(Group group, Account account) {
		return group != null && group.owner.equals(account);
	}

	/**
	 * Returns true, if the currently logged in account is allowed to create a course.
	 *
	 * @return True, if account is allowed to create course
	 */
	public static boolean createCourse() {
		Account current = Component.currentAccount();
		return current.role == AccountRole.TUTOR || current.role == AccountRole.ADMIN;
	}

	/**
	 * Returns true, if the currently logged in account is allowed to view a specific group.
	 *
	 * @param group Group to view
	 * @return True, if currently logged in account is allowed to view group
	 */
	public static boolean viewGroup(Group group) {
		Account current = Component.currentAccount();

		if (group == null) {
			return false;
		}

		if (Secured.isAdmin()) {
			return true;
		}

		switch (group.groupType) {
			case open:
				return true;

			case close:
				if (Secured.isMemberOfGroup(group, current)) {
					return true;
				}
			case course:
				if (Secured.isMemberOfGroup(group, current)) {
					return true;
				}
			default:
				return false;
		}
	}

	/**
	 * Returns true, if the currently logged in account is allowed to edit a specific group.
	 *
	 * @param group Group
	 * @return True, if logged in account is allowed to edit group
	 */
	public static boolean editGroup(Group group) {
		Account current = Component.currentAccount();

		return group != null && (Secured.isAdmin() || Secured.isOwnerOfGroup(group, current));
	}

	/**
	 * Returns true, if the currently logged in account is allowed to delete a specific group.
	 *
	 * @param group Group
	 * @return True, if logged in account is allowed to delete group
	 */
	public static boolean deleteGroup(Group group) {
		Account current = Component.currentAccount();

		return group != null && (Secured.isAdmin() || Secured.isOwnerOfGroup(group, current));
	}

	/**
	 * Returns true, if the currently logged in account is allowed to remove a specific account from a group.
	 *
	 * @param group Group
	 * @param account Account to remove
	 * @return True, if logged in account is allowed to remove account from group
	 */
	public static boolean removeGroupMember(Group group, Account account) {
		Account current = Component.currentAccount();

		return group != null
				&& !Secured.isOwnerOfGroup(group, account)
				&& (Secured.isAdmin() || Secured.isOwnerOfGroup(group, current) || current.equals(account));
	}

	/**
	 * Returns true, if currently logged in account is allowed to invite a member to a specific group.
	 *
	 * @param group Group to invite
	 * @return True, if logged in account is allowed to invite to group
	 */
	public static boolean inviteMember(Group group) {
		Account current = Component.currentAccount();

		return group != null && (Secured.isOwnerOfGroup(group, current) || Secured.isAdmin());
	}

	/**
	 * Returns true, if the currently logged in account is allowed to accept an invitation.
	 *
	 * @param groupAccount Group account
	 * @return True, if logged in account is allowed to accept an invitation
	 */
	public static boolean acceptInvitation(GroupAccount groupAccount) {
		Account current = Component.currentAccount();

		return groupAccount != null && (Secured.isAdmin() || groupAccount.account.equals(current));
	}

	/**
	 * Returns true, if the currently logged in account is allowed to view a specific post.
	 *
	 * @param post Post to view
	 * @return True, if logged in account is allowed to view post
	 */
	public static boolean viewPost(Post post) {
		Account current = Component.currentAccount();
		
		if (post == null) {
			return false;
		}
		
		if (Secured.isAdmin()) {
			return true;
		}
		
		if(post.belongsToAccount()) {
			return post.account.equals(current) || Secured.isFriend(post.account);
		}

		if (post.belongsToGroup()) {
			switch (post.group.groupType) {
				case open:
					return true;

				case close:
					if (Secured.isMemberOfGroup(post.group, current)) {
						return true;
					}
				case course:
					if (Secured.isMemberOfGroup(post.group, current)) {
						return true;
					}
				default:
					return false;
			}
		}
		
		return false;
	}

	/**
	 * Returns true, if currently logged in account is allowed to delete a specific post.
	 *
	 * @param post Post
	 * @param account Account
	 * @return True, if logged in account is allowed to delete post
	 */
	public static boolean isAllowedToDeletePost(Post post, Account account) {
		if (post == null) {
			return false;
		}

		if (Secured.isAdmin()) {
			return true;
		}

		if (Secured.isOwnerOfPost(post, account)) {
			return true;
		}

		// is Post
		if (post.belongsToGroup()) {
			if (Secured.isOwnerOfGroup(post.group, account)) {
				return true;
			}
		}

		if (post.belongsToAccount()) {
			if (post.account.equals(account)) {
				return true;
			}
		}

		// is Comment
		return post.parent != null && Secured.isAllowedToDeletePost(post.parent, account);
	}

	/**
	 * Returns true, if an account is the owner of a post.
	 *
	 * @param post Post
	 * @param account Account
	 * @return True, if account is owner of post
	 */
	public static boolean isOwnerOfPost(Post post, Account account) {
		return post != null && post.owner.equals(account);
	}

	/**
	 * Returns true, if the currently logged in account is allowed to view comments on a specific post.
	 *
	 * @param post Post to view comments
	 * @return True, if logged in account is allowed to view comments of post
	 */
	public static boolean viewComments(Post post) {
		Account current = Component.currentAccount();
		
		if (post == null) {
			return false;
		}
		
		if (Secured.isAdmin()) {
			return true;
		}

		if (post.belongsToAccount()) {
			return post.account.equals(current) || Secured.isFriend(post.account);
		}
		
		if (post.belongsToGroup()) {
			switch (post.group.groupType) {
				case open:
					return true;

				case close:
					if (Secured.isMemberOfGroup(post.group, current)) {
						return true;
					}
				case course:
					if (Secured.isMemberOfGroup(post.group, current)) {
						return true;
					}
				default:
					return false;
			}
		}
		
		return false;
	}

	/**
	 * Returns true, if the currently logged in account is allowed to add a comment to a specific post.
	 *
	 * @param post Post to comment
	 * @return True, if logged in account is allowed to add comment
	 */
	public static boolean addComment(Post post) {
		Account current = Component.currentAccount();

		if (post == null) {
			return false;
		}

		if (Secured.isAdmin()) {
			return true;
		}

		if (post.belongsToGroup()) {
			return Secured.isMemberOfGroup(post.group, current);
		}

		return post.belongsToAccount() && (post.account.equals(current) || Secured.isFriend(post.account));
	}

	/**
	 * Returns true, if the currently logged in account is owner of an account by ID.
	 *
	 * @param accountId Account ID to check
	 * @return True, if logged in user is owner of account
	 */
	public static boolean isOwnerOfAccount(final Long accountId) {
		return Account.isOwner(accountId, Component.currentAccount());
	}

	/**
	 * Returns true, if the currently logged in account has a friendship with a specific account.
	 *
	 * @param account Account to check friendship
	 * @return True, if logged in account has friendship to account
	 */
	public static boolean isFriend(Account account) {
		return Friendship.alreadyFriendly(Component.currentAccount(), account);
	}

	/**
	 * Returns true, if the currently logged in account is allowed to edit a specific account.
	 *
	 * @param account Account to edit
	 * @return True, if logged in account is allowed to edit account
	 */
	public static boolean editAccount(Account account) {
		return Component.currentAccount().equals(account);
	}

	/**
	 * Returns true, if the currently logged in account is allowed to upload media into a specific group.
	 *
	 * @param group Group to upload media to
	 * @return True, if logged in account is allowed to upload media to group
	 */
	public static boolean uploadMedia(Group group) {
		Account current = Component.currentAccount();

		if (group == null) {
			return false;
		}

		if (Secured.isAdmin()) {
			return true;
		}

		switch (group.groupType) {
			case open:
				if (Secured.isMemberOfGroup(group, current)) {
					return true;
				}

			case close:
				if (Secured.isMemberOfGroup(group, current)) {
					return true;
				}
			case course:
				if (Secured.isOwnerOfGroup(group, current)) {
					return true;
				}
			default:
				return false;
		}
	}

	/**
	 * Returns true, if the currently logged in account is allowed to view a specific media.
	 *
	 * @param media Media to view
	 * @return True, if logged in account is allowed to view media
	 */
	public static boolean viewMedia(Media media) {
        return media != null && Secured.viewGroup(media.group);
	}

	/**
	 * Returns true, if the currently logged in account is allowed to delete media from its associated group.
	 *
	 * @param media Media to be deleted
	 * @return True, if logged in account is allowed to delete media
	 */
	public static boolean deleteMedia(Media media) {
		Account current = Component.currentAccount();
		Group group = media.group;
        
        return Secured.isAdmin() || Secured.isOwnerOfGroup(group, current) || media.owner.equals(current);
    }

    /**
     * Returns true, if the current user has access to a notification.
     *
     * @param notification Notification to be checked
     * @return True, if user has access
     */
	public static boolean hasAccessToNotification(Notification notification) {
		return notification.recipient.equals(Component.currentAccount());
	}
}
