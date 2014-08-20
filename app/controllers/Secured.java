package controllers;


import java.util.Date;

import models.*;
import models.enums.AccountRole;
import play.Logger;
import play.Play;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;
import views.html.login;

public class Secured extends Security.Authenticator {

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
                return null;
            } 
        }
 
        // update time in session
        String tickString = Long.toString(new Date().getTime());
        ctx.session().put("userTime", tickString);
		return ctx.session().get("id");

    }
	
	@Override
    public Result onUnauthorized(Context ctx) {
		Logger.info("Unauthorized - Redirect to Login");
		return ok(login.render());
    }
		

	
	public static boolean isAdmin() {
		Account current = Component.currentAccount();
		if (current.role == AccountRole.ADMIN) {
			return true;
		} else {
			return false;
		}
	}
	
	/*
	 * GROUP
	 */
	
	public static boolean isMemberOfGroup(Group group, Account account){
		return Group.isMember(group, account);
	}
	
	public static boolean isOwnerOfGroup(Group group, Account account){
		if(group != null){
			return group.owner.equals(account);
		} else {
			return false;
		}
	}
	
	public static boolean createCourse() {
		Account current = Component.currentAccount();
		if(current.role == AccountRole.TUTOR || current.role == AccountRole.ADMIN) {
			return true;
		} else {
			return false;
		}
	}
	
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
	
	public static boolean editGroup(Group group) {
		Account current = Component.currentAccount();

		if (group == null) {
			return false;
		}

		if (Secured.isAdmin()) {
			return true;
		}

		if (Secured.isOwnerOfGroup(group, current)) {
			return true;
		}
		
		return false;
		
	}
	
	public static boolean deleteGroup(Group group) {
		Account current = Component.currentAccount();

		if (group == null) {
			return false;
		}

		if (Secured.isAdmin()) {
			return true;
		}

		if (Secured.isOwnerOfGroup(group, current)) {
			return true;
		}
		
		return false;
		
	}
	
	public static boolean removeGroupMember(Group group, Account account) {
		Account current = Component.currentAccount();
		
		if (group == null) {
			return false;
		}
		
		if(Secured.isOwnerOfGroup(group, account)) {
			return false;
		}

		if (Secured.isAdmin()) {
			return true;
		}

		if (Secured.isOwnerOfGroup(group, current)) {
			return true;
		}
		
		if(current.equals(account)) {
			return true;
		}
		
		return false;
		
	}
	
	public static boolean inviteMember(Group group) {
		Account current = Component.currentAccount();
		
		if (group == null) {
			return false;
		}
		
		if(Secured.isOwnerOfGroup(group, current)) {
			return true;
		}
		
		if (Secured.isAdmin()) {
			return true;
		}
		
		return false;
	}
	
	public static boolean acceptInvitation(GroupAccount groupAccount) {
		Account current = Component.currentAccount();
		
		if (groupAccount == null) {
			return false;
		}
				
		if (Secured.isAdmin()) {
			return true;
		}
		
		if (groupAccount.account.equals(current)) {
			return true;
		}
		
		return false;
	}
	
	

	/*
	 * POST
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
			if(post.account.equals(current)) {
				return true;
			}
			if(Secured.isFriend(post.account)) {
				return true;
			}
			return false;
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
	
	
	public static boolean isAllowedToDeletePost(Post post, Account account){		
		if(post == null) {
			return false;
		}
		
		if (Secured.isAdmin()) {
			return true;
		}

		if(Secured.isOwnerOfPost(post, account)) {
			return true;
		}
		
		// Is Post
		if(post.belongsToGroup()) {
			if(Secured.isOwnerOfGroup(post.group, account)) {
				return true;
			}
		}
		
		if(post.belongsToAccount()) {
			if(post.account.equals(account)){
				return true;
			}
		}
		
		// Is Comment
		if(post.parent != null) {
			return Secured.isAllowedToDeletePost(post.parent, account);
		}
		return false;
		
	}
	
	
//	public static boolean isAllowedToDeletePost(final Post post, final Account account){
//		if(post != null && post.account != null){
//			if(isOwnerOfPost(post, account) || post.account.equals(account)){
//				return true;
//			}
//		}else if(post != null && post.parent != null && post.parent.account != null){
//			if (isOwnerOfPost(post, account) || post.parent.account.equals(account)){
//				return true;
//			}
//		}else if(post != null && isOwnerOfPost(post, account)){
//			return true;
//		}
//		return false;
//	}
	
	public static boolean isOwnerOfPost(Post post, Account account){
		if(post != null){
			return post.owner.equals(account);
		}
		return false;
	}
	
	public static boolean viewComments(Post post) {
		Account current = Component.currentAccount();
		
		if (post == null) {
			return false;
		}
		
		if (Secured.isAdmin()) {
			return true;
		}

		if(post.belongsToAccount()) {
			if(post.account.equals(current)) {
				return true;
			}
			if(Secured.isFriend(post.account)) {
				return true;
			}
			return false;
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
	
	public static boolean addComment(Post post) {
		Account current = Component.currentAccount();
		
		if (post == null) {
			return false;
		}
		
		if (Secured.isAdmin()) {
			return true;
		}
		
		if(post.belongsToGroup()) {
            return Secured.isMemberOfGroup(post.group, current);
		}
		
		if(post.belongsToAccount()) {
			if(post.account.equals(current)) {
				return true;
			}
			if(Secured.isFriend(post.account)) {
				return true;
			}
			return false;
		}
		
		
		return false;
	}
	
	
	/*
	 * ACCOUNT
	 * 
	 */
	
	
	public static boolean isOwnerOfAccount(final Long accountId) {
		return Account.isOwnerTransactional(accountId, Component.currentAccount());
	}

	public static boolean isFriend(Account account) {
		return Friendship.alreadyFriendlyTransactional(Component.currentAccount(), account);
	}

	public static boolean editAccount(Account account) {
		if(Component.currentAccount().equals(account)){
			return true;
		} else {
			return false;
		}
	}
	
	
	/*
	 * MEDIA
	 */
		
	public static boolean isOwnerOfMedia(final Long mediaId) {
		return Media.isOwner(mediaId, Component.currentAccount());
	}

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

	public static boolean viewMedia(Media media) {
        return media != null && Secured.viewGroup(media.group);
	}

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
	public static boolean hasAccessToNotification(NewNotification notification) {
		return notification.recipient.equals(Component.currentAccount());
	}
}
