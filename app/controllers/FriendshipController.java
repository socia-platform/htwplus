package controllers;

import java.util.List;

import controllers.Navigation.Level;
import models.*;
import models.enums.AccountRole;
import models.enums.LinkType;
import models.services.NotificationService;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Friends.*;

@Security.Authenticated(Secured.class)
@Transactional
public class FriendshipController extends BaseController {

	public static Result index() {
		Navigation.set(Level.FRIENDS, "Übersicht");
		Account currentUser = Component.currentAccount();
		List<Account> friends = Friendship.findFriends(currentUser);
		
		// find requests and add rejects to simplify view output
		List<Friendship> requests = Friendship.findRequests(currentUser);
		requests.addAll(Friendship.findRejects(currentUser));
		
		return ok(index.render(friends,requests));
	}

    /**
     * Creates a friendship request.
     *
     * @param friendId ID of potential friend
     * @return SimpleResult redirect
     */
	public static Result requestFriend(long friendId) {
		Account currentUser = Component.currentAccount();
		Account potentialFriend = Account.findById(friendId);
		
		if (hasLogicalErrors(currentUser,potentialFriend)) {
			return redirect(controllers.routes.FriendshipController.index());
		}
		
		Friendship friendship = new Friendship(currentUser, potentialFriend, LinkType.request);
		friendship.create();
        NotificationService.getInstance().createNotification(friendship, Friendship.FRIEND_NEW_REQUEST);

        flash("success","Deine Einladung wurde verschickt!");
		return redirect(controllers.routes.FriendshipController.index());
		
	}

	public static Result deleteFriend(long friendId) {
		Account currentUser = Component.currentAccount();
		Account friend = Account.findById(friendId);
		
		if (friend == null) {
			flash("error","Diesen User gibt es nicht!");
			return redirect(controllers.routes.FriendshipController.index());
		}
		
		Friendship friendshipLink = Friendship.findFriendLink(currentUser, friend);
		Friendship reverseLink = Friendship.findFriendLink(friend, currentUser);
		
		if (friendshipLink == null || reverseLink == null) {
			flash("error","Diese Freundschaft besteht nicht!");
		} else {
			friendshipLink.delete();
			reverseLink.delete();
			flash("success","Tja, das war es dann wohl :-/");
		}
		
		return redirect(controllers.routes.FriendshipController.index());
	}

    /**
     * Accepts a friend request.
     *
     * @param friendId ID of friend
     * @return SimpleResult redirect
     */
	public static Result acceptFriendRequest(long friendId) {
		Account currentUser = Component.currentAccount();
		Account potentialFriend = Account.findById(friendId);
		
		// establish connection based on three actions
		// first, check if currentAccount got an request
		Friendship requestLink = Friendship.findRequest(potentialFriend,currentUser);
		
		if (requestLink == null) {
			flash("info","Es gibt keine Freundschaftsanfrage von diesem User");
			return redirect(controllers.routes.FriendshipController.index());
		} else {
			// if so: set LinkType from request to friend
			requestLink.linkType = LinkType.establish;
			requestLink.update();
			
			// and create new friend-connection between currentAccount and requester
            Friendship friendship = new Friendship(currentUser, potentialFriend, LinkType.establish);
            friendship.create();
            NotificationService.getInstance().createNotification(friendship, Friendship.FRIEND_REQUEST_SUCCESS);

            flash("success", "Freundschaft erfolgreich hergestellt!");
		}
		
		return redirect(controllers.routes.FriendshipController.index());
	}

    /**
     * Declines a friend request.
     *
     * @param friendshipId ID of rejected friend
     * @return SimpleResult redirect
     */
	public static Result declineFriendRequest(long friendshipId) {
		Friendship requestLink = Friendship.findById(friendshipId);
		if (requestLink != null && requestLink.friend.equals(Component.currentAccount())) {
			requestLink.linkType = LinkType.reject;
            requestLink.update();
            NotificationService.getInstance().createNotification(requestLink, Friendship.FRIEND_REQUEST_DECLINE);
		}

		return redirect(controllers.routes.FriendshipController.index());
	}

	public static Result cancelFriendRequest(long friendshipId) {
		Friendship friendship = Friendship.findById(friendshipId);
		if (friendship != null && friendship.account.equals(Component.currentAccount())) {
			friendship.delete();
		} else {
			flash("error","Diese Freundschaftsanfrage gibt es nicht!");
		}
		
		return redirect(controllers.routes.FriendshipController.index());
	}
	
	private static boolean hasLogicalErrors(Account currentUser, Account potentialFriend) {
		if (potentialFriend.equals(currentUser)) {
			flash("info","Du kannst nicht mit dir befreundet sein!");
			return true;
		}

        if (potentialFriend.role == AccountRole.DUMMY) {
            flash("error", "Mit diesem Account kannst du nicht befreundet sein!");
            return true;
        }
		
		if (Friendship.findRequest(currentUser,potentialFriend) != null) {
			flash("info","Deine Freundschaftsanfrage wurde bereits verschickt!");
			return true;
		}
		
		if (Friendship.findReverseRequest(currentUser,potentialFriend) != null) {
			flash("info","Du hast bereits eine Freundschaftsanfrage von diesem User. Schau mal nach ;-)");
			return true;
		}
		
		if (Friendship.alreadyFriendly(currentUser, potentialFriend)) {
			flash("info","Ihr seid bereits Freunde!");
			return true;
		}
		
		if (Friendship.alreadyRejected(currentUser, potentialFriend)) {
			flash("info","Deine Freundschaftsanfrage wurde bereits abgelehnt. "
					+ "Bestätige die Ablehnung und dann kannst du es noch einmal versuchen.");
			return true;
		} 
		
		return false;
	}

}
