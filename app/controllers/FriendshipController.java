package controllers;

import controllers.Navigation.Level;
import managers.AccountManager;
import managers.FriendshipManager;
import models.Account;
import models.Friendship;
import models.enums.AccountRole;
import models.enums.LinkType;
import models.services.NotificationService;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Friends.index;

import javax.inject.Inject;
import java.util.List;

@Security.Authenticated(Secured.class)
@Transactional
public class FriendshipController extends BaseController {

    @Inject
    FriendshipManager friendshipManager;

    @Inject
    AccountManager accountManager;

    public Result index() {
        Navigation.set(Level.FRIENDS, "Übersicht");
        Account currentUser = Component.currentAccount();
        List<Account> friends = friendshipManager.findFriends(currentUser);

        // find requests and add rejects to simplify view output
        List<Friendship> requests = friendshipManager.findRequests(currentUser);
        requests.addAll(friendshipManager.findRejects(currentUser));

        return ok(index.render(friends, requests));
    }

    /**
     * Creates a friendship request.
     *
     * @param friendId ID of potential friend
     * @return SimpleResult redirect
     */
    public Result requestFriend(long friendId) {
        Account currentUser = Component.currentAccount();
        Account potentialFriend = accountManager.findById(friendId);

        if (hasLogicalErrors(currentUser, potentialFriend)) {
            return redirect(controllers.routes.FriendshipController.index());
        }

        Friendship friendship = new Friendship(currentUser, potentialFriend, LinkType.request);
        friendshipManager.create(friendship);
        NotificationService.getInstance().createNotification(friendship, Friendship.FRIEND_NEW_REQUEST);

        flash("success", "Deine Einladung wurde verschickt!");
        return redirect(controllers.routes.FriendshipController.index());

    }

    public Result deleteFriend(long friendId) {
        Account currentUser = Component.currentAccount();
        Account friend = accountManager.findById(friendId);

        if (friend == null) {
            flash("error", "Diesen User gibt es nicht!");
            return redirect(controllers.routes.FriendshipController.index());
        }

        Friendship friendshipLink = friendshipManager.findFriendLink(currentUser, friend);
        Friendship reverseLink = friendshipManager.findFriendLink(friend, currentUser);

        if (friendshipLink == null || reverseLink == null) {
            flash("error", "Diese Kontaktverbindung besteht nicht!");
        } else {
            friendshipManager.delete(friendshipLink);
            friendshipManager.delete(reverseLink);
            flash("success", "Tja, das war es dann wohl :-/");
        }

        return redirect(controllers.routes.FriendshipController.index());
    }

    /**
     * Accepts a friend request.
     *
     * @param friendId ID of friend
     * @return SimpleResult redirect
     */
    public Result acceptFriendRequest(long friendId) {
        Account currentUser = Component.currentAccount();
        Account potentialFriend = accountManager.findById(friendId);

        // establish connection based on three actions
        // first, check if currentAccount got an request
        Friendship requestLink = friendshipManager.findRequest(potentialFriend, currentUser);

        if (requestLink == null) {
            flash("info", "Es gibt keine Kontaktanfrage von diesem Benutzer");
            return redirect(controllers.routes.FriendshipController.index());
        } else {
            // if so: set LinkType from request to friend
            requestLink.linkType = LinkType.establish;
            friendshipManager.update(requestLink);

            // and create new friend-connection between currentAccount and requester
            Friendship friendship = new Friendship(currentUser, potentialFriend, LinkType.establish);
            friendshipManager.create(friendship);
            NotificationService.getInstance().createNotification(friendship, Friendship.FRIEND_REQUEST_SUCCESS);

            flash("success", "Kontakt erfolgreich hergestellt!");
        }

        return redirect(controllers.routes.FriendshipController.index());
    }

    /**
     * Declines a friend request.
     *
     * @param friendshipId ID of rejected friend
     * @return SimpleResult redirect
     */
    public Result declineFriendRequest(long friendshipId) {
        Friendship requestLink = friendshipManager.findById(friendshipId);
        if (requestLink != null && requestLink.friend.equals(Component.currentAccount())) {
            requestLink.linkType = LinkType.reject;
            friendshipManager.update(requestLink);
            NotificationService.getInstance().createNotification(requestLink, Friendship.FRIEND_REQUEST_DECLINE);
        }

        return redirect(controllers.routes.FriendshipController.index());
    }

    public Result cancelFriendRequest(long friendshipId) {
        Friendship friendship = friendshipManager.findById(friendshipId);
        if (friendship != null && friendship.account.equals(Component.currentAccount())) {
            friendshipManager.delete(friendship);
        } else {
            flash("error", "Diese Kontaktanfrage gibt es nicht!");
        }

        return redirect(controllers.routes.FriendshipController.index());
    }

    private boolean hasLogicalErrors(Account currentUser, Account potentialFriend) {
        if (potentialFriend.equals(currentUser)) {
            flash("info", "Du kannst nicht mit dir selbst in Kontakt stehen!");
            return true;
        }

        if (potentialFriend.role == AccountRole.DUMMY) {
            flash("error", "Mit diesem Account kannst du nicht in Kontakt stehen!");
            return true;
        }

        if (friendshipManager.findRequest(currentUser, potentialFriend) != null) {
            flash("info", "Deine Kontaktanfrage wurde bereits verschickt!");
            return true;
        }

        if (friendshipManager.findReverseRequest(currentUser, potentialFriend) != null) {
            flash("info", "Du hast bereits eine Kontaktanfrage von diesem User. Schau mal nach ;-)");
            return true;
        }

        if (friendshipManager.alreadyFriendly(currentUser, potentialFriend)) {
            flash("info", "Ihr steht bereits in Kontakt!");
            return true;
        }

        if (friendshipManager.alreadyRejected(currentUser, potentialFriend)) {
            flash("info", "Deine Kontaktanfrage wurde bereits abgelehnt. "
                    + "Bestätige die Ablehnung und dann kannst du es noch einmal versuchen.");
            return true;
        }

        return false;
    }

}
