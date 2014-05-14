package controllers;

import java.util.List;

import controllers.Navigation.Level;
import models.Account;
import models.Friendship;
import models.Notification;
import models.Notification.NotificationType;
import models.enums.LinkType;
import play.Logger;
import play.db.jpa.Transactional;
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
	
	public static Result requestFriend(long friendId){
		Account currentUser = Component.currentAccount();
		Account potentialFriend = Account.findById(friendId);
		
		if(hasLogicalErrors(currentUser,potentialFriend)){
			return redirect(routes.FriendshipController.index());
		}
		
		Friendship friendship = new Friendship(currentUser,potentialFriend,LinkType.request);
		friendship.create();
		Notification.newNotification(NotificationType.FRIEND_NEW_REQUEST, currentUser.id, potentialFriend);
		flash("success","Deine Einladung wurde verschickt!");
		return redirect(routes.FriendshipController.index());
		
	}
	
	public static Result deleteFriend(long friendId){
		Account currentUser = Component.currentAccount();
		Account friend = Account.findById(friendId);
		
		if(friend == null){
			flash("error","Diesen User gibt es nicht!");
			return redirect(routes.FriendshipController.index());
		}
		
		Friendship friendshipLink = Friendship.findFriendLink(currentUser, friend);
		Friendship reverseLink = Friendship.findFriendLink(friend, currentUser);
		
		if(friendshipLink == null || reverseLink == null){
			flash("error","Diese Freundschaft besteht nicht!");
		} else {
			friendshipLink.delete();
			reverseLink.delete();
			flash("success","Tja, das war es dann wohl :-/");
		}
		
		return redirect(routes.FriendshipController.index());
	}
	
	public static Result acceptFriendRequest(long friendId){
		Account currentUser = Component.currentAccount();
		Account potentialFriend = Account.findById(friendId);
		
		// establish connection based on three actions
		// first, check if currentAccount got an request
		Friendship requestLink = Friendship.findRequest(potentialFriend,currentUser);
		
		if(requestLink == null){
			flash("info","Es gibt keine Freundschaftsanfrage von diesem User");
			return redirect(routes.FriendshipController.index());
		} else{
			// if so: set LinkType from request to friend
			requestLink.linkType = LinkType.establish;
			requestLink.update();
			
			// and create new friend-connection between currentAccount and requester
			new Friendship(currentUser,potentialFriend,LinkType.establish).create();
			Notification.newNotification(NotificationType.FRIEND_REQUEST_SUCCESS, currentUser.id, potentialFriend);
			flash("success","Freundschaft erfolgreich hergestellt!");
		}
		
		
		
		return redirect(routes.FriendshipController.index());
	}
	
	public static Result declineFriendRequest(long friendshipId){
		Friendship requestLink = Friendship.findById(friendshipId);
		if(requestLink != null && requestLink.friend.equals(Component.currentAccount())){
			requestLink.linkType = LinkType.reject;
			requestLink.update();
			Notification.newNotification(NotificationType.FRIEND_REQUEST_DECLINE, requestLink.friend.id, requestLink.account);
		}
		
		
		return redirect(routes.FriendshipController.index());
	}
	
	public static Result cancelFriendRequest(long friendshipId){
		
		Friendship friendship = Friendship.findById(friendshipId);
		if(friendship != null && friendship.account.equals(Component.currentAccount())){
			friendship.delete();
		} else {
			flash("error","Diese Freundschaftsanfrage gibt es nicht!");
		}
		
		return redirect(routes.FriendshipController.index());
	}
	
	private static boolean hasLogicalErrors(Account currentUser, Account potentialFriend) {
		if(potentialFriend.equals(currentUser)){
			flash("info","Du kannst nicht mit dir befreundet sein!");
			return true;
		}
		
		if(Friendship.findRequest(currentUser,potentialFriend) != null){
			flash("info","Deine Freundschaftsanfrage wurde bereits verschickt!");
			return true;
		}
		
		if(Friendship.findReverseRequest(currentUser,potentialFriend) != null){
			flash("info","Du hast bereits eine Freundschaftsanfrage von diesem User. Schau mal nach ;-)");
			return true;
		}
		
		if(Friendship.alreadyFriendly(currentUser,potentialFriend)){
			flash("info","Ihr seid bereits Freunde!");
			return true;
		}
		
		if(Friendship.alreadyRejected(currentUser, potentialFriend)) {
			flash("info","Deine Freundschaftsanfrage wurde bereits abgelehnt. "
					+ "Bestätige die Ablehnung und dann kannst du es noch einmal versuchen.");
			return true;
		} 
		
		return false;
	}

}
