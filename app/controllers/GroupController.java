package controllers;

import java.util.Collection;
import java.util.List;

import controllers.Navigation.Level;
import models.Account;
import models.Friendship;
import models.Group;
import models.GroupAccount;
import models.Media;
import models.Notification;
import models.Notification.NotificationType;
import models.Post;
import models.enums.GroupType;
import models.enums.LinkType;
import play.Logger;
import play.Play;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Call;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Group.index;
import views.html.Group.media;
import views.html.Group.view;
import views.html.Group.create;
import views.html.Group.edit;
import views.html.Group.token;
import views.html.Group.invite;


@Transactional
@Security.Authenticated(Secured.class)
public class GroupController extends BaseController {

	static Form<Group> groupForm = Form.form(Group.class);
	static Form<Post> postForm = Form.form(Post.class);
	static final int LIMIT = Integer.parseInt(Play.application().configuration().getString("htwplus.post.limit"));
	static final int PAGE = 1;
	
	public static Result index() {
		Navigation.set(Level.GROUPS, "Übersicht");
		Account account = Component.currentAccount();
		List<GroupAccount> groupRequests = GroupAccount.findRequests(account);
		List<Group> groupAccounts = GroupAccount.findGroupsEstablished(account);
		List<Group> courseAccounts = GroupAccount.findCoursesEstablished(account);
		return ok(index.render(groupAccounts,courseAccounts,groupRequests,groupForm));
	}
		
	@Transactional(readOnly=true)
	public static Result view(Long id, int page) {
		Logger.info("Show group with id: " +id);
		Group group = Group.findById(id);
		if(!Secured.viewGroup(group)){
			return redirect(controllers.routes.Application.index());
		}
		
		if (group == null) {
			Logger.error("No group found with id: " +id);
			return redirect(controllers.routes.GroupController.index());
		} else {
			Navigation.set(Level.GROUPS, "Newsstream", group.title, controllers.routes.GroupController.view(group.id, PAGE));
			Logger.info("Found group with id: " +id);
			List<Post> posts = Post.getPostsForGroup(group, LIMIT, page);
			return ok(view.render(group, posts, postForm, Post.countPostsForGroup(group), LIMIT, page));
		}
	}
	
	@Transactional(readOnly=true)
	public static Result media(Long id) {
		Form<Media> mediaForm = Form.form(Media.class);
		Group group = Group.findById(id);
		
		if(!Secured.viewGroup(group)){
			return redirect(controllers.routes.Application.index());
		}
		
		if (group == null) {
			return redirect(controllers.routes.GroupController.index());
		} else {
			Navigation.set(Level.GROUPS, "Media", group.title, controllers.routes.GroupController.view(group.id, PAGE));
			List<Media> mediaSet = group.media; 
			return ok(media.render(group, mediaForm, mediaSet));
		}
	}
	
	public static Result create() {
		Navigation.set(Level.GROUPS, "Erstellen");
		return ok(create.render(groupForm));
	}

	public static Result add() {	
		Navigation.set(Level.GROUPS, "Erstellen");
		
		// Get data from request
		Form<Group> filledForm = groupForm.bindFromRequest();
		
		// Perform JPA Validation
		if (filledForm.hasErrors()) {
			return badRequest(create.render(filledForm));
		} else {
			Group group = filledForm.get();
			int groupType;
			try {
				groupType = Integer.parseInt(filledForm.data().get("type"));
			} catch (NumberFormatException ex){
				filledForm.reject("type", "Bitte eine Sichtbarkeit wählen!");
				return ok(create.render(filledForm));
			}
			
			String successMsg;
			switch(groupType){
			
				case 0: group.groupType = GroupType.open; 
						successMsg = "Öffentliche Gruppe"; 
						break;
						
				case 1: group.groupType = GroupType.close; 
						successMsg = "Geschlossene Gruppe";
						break;
						
				case 2: group.groupType = GroupType.course;
						successMsg = "Kurs";
						String token = filledForm.data().get("token");
						if(!Group.validateToken(token)){
							filledForm.reject("token","Bitte einen Token zwischen 4 und 45 Zeichen eingeben!");
							return ok(create.render(filledForm));
						}
						
						if(!Secured.createCourse()) {
							flash("error", "Du darfst leider keinen Kurs erstellen");
							return badRequest(create.render(filledForm));
						}
						break;
						
				default: 
					filledForm.reject("Nicht möglich!");
					return ok(create.render(filledForm));
			}
			
			group.createWithGroupAccount(Component.currentAccount());
			flash("success", successMsg+" erstellt!");
			return redirect(controllers.routes.GroupController.view(group.id, PAGE));
		}
	}

	@Transactional
	public static Result edit(Long id) {
		Group group = Group.findById(id);
		
		
		if (group == null) {
			return redirect(controllers.routes.GroupController.index());
		} else {
			Navigation.set(Level.GROUPS, "Bearbeiten", group.title, controllers.routes.GroupController.view(group.id, PAGE));
			Form<Group> groupForm = Form.form(Group.class).fill(group);
			groupForm.data().put("type", String.valueOf(group.groupType.ordinal()));
			return ok(edit.render(group, groupForm));
		}
	}
	
	@Transactional
	public static Result update(Long groupId) {
		Group group = Group.findById(groupId);
		Navigation.set(Level.GROUPS, "Bearbeiten", group.title, controllers.routes.GroupController.view(group.id, PAGE));
		
		// Check rights
		if(!Secured.editGroup(group)) {
			return redirect(controllers.routes.GroupController.index());
		}
		
		Form<Group> filledForm = groupForm.bindFromRequest();
		int groupType = Integer.parseInt(filledForm.data().get("type"));
		String description = filledForm.data().get("description");

		switch(groupType){
			case 0: group.groupType = GroupType.open; 
					group.token = null;
					break;
			case 1: group.groupType = GroupType.close; 
					group.token = null;
					break;
			case 2: group.groupType = GroupType.course; 
					String token = filledForm.data().get("token");
					if(!Group.validateToken(token)){
						filledForm.reject("token","Bitte einen Token zwischen 4 und 45 Zeichen eingeben!");
						return ok(edit.render(group, filledForm));
					}					
					if(!Secured.createCourse()) {
						flash("error", "Du darfst leider keinen Kurs erstellen");
						return badRequest(edit.render(group, filledForm));
					}	
					group.token = token;
					break;
			default:
				filledForm.reject("Nicht möglich!");
				return ok(edit.render(group, filledForm));
		}
		group.description = description;
		group.update();
		flash("success", "'" + group.title + "' erfolgreich bearbeitet!");
		return redirect(controllers.routes.GroupController.view(groupId, PAGE));
		
	}
	
	public static Result delete(Long id) {
		Group group = Group.findById(id);
		if(Secured.deleteGroup(group)){
			group.delete();
			flash("info", "'" + group.title + "' wurde erfolgreich gelöscht!");
		} else {
			flash("error", "Dazu hast du keine Berechtigung!");
		}
		return redirect(controllers.routes.GroupController.index());
	}
	
	public static List<Group> showAll() {
		return Group.all();
	}
	
	public static Result token(Long groupId) {
		Group group = Group.findById(groupId);
		Navigation.set(Level.GROUPS, "Token eingeben", group.title, controllers.routes.GroupController.view(group.id, PAGE));
		return ok(token.render(group, groupForm));
	}
	
	public static Result validateToken(Long groupId) {
		Group group = Group.findById(groupId);
		
		if(Secured.isMemberOfGroup(group, Component.currentAccount())){
			flash("error", "Du bist bereits Mitglied dieser Gruppe!");
			return redirect(controllers.routes.GroupController.view(groupId, PAGE));
		}
		
		Navigation.set(Level.GROUPS, "Token eingeben", group.title, controllers.routes.GroupController.view(group.id, PAGE));
		Form<Group> filledForm = groupForm.bindFromRequest();
		String enteredToken = filledForm.data().get("token");
		
		if(enteredToken.equals(group.token)){
			Account account = Component.currentAccount();
			GroupAccount groupAccount = new GroupAccount(account, group, LinkType.establish);
			groupAccount.create();
			flash("success", "Kurs erfolgreich beigetreten!");
			return redirect(controllers.routes.GroupController.view(groupId, PAGE));
		} else {
			flash("error", "Hast du dich vielleicht vertippt? Der Token ist leider falsch.");
			return badRequest(token.render(group, filledForm));
		}
	}
	
	
	public static Result join(long id){
		Account account = Component.currentAccount();
		Group group = Group.findById(id);
		GroupAccount groupAccount;
				
		if(Secured.isMemberOfGroup(group, account)){
			Logger.debug("User is already member of group or course");
			flash("error", "Du bist bereits Mitglied dieser Gruppe!");
			return redirect(controllers.routes.GroupController.view(id, PAGE));
		}
		
		// is already requested?
		groupAccount = GroupAccount.find(account, group);
		if(groupAccount != null && groupAccount.linkType.equals(LinkType.request) ){
			flash("info", "Deine Beitrittsanfrage wurde bereits verschickt!");
			return redirect(controllers.routes.GroupController.index());
		}
		
		if(groupAccount != null && groupAccount.linkType.equals(LinkType.reject)){
			flash("error", "Deine Beitrittsanfrage wurde bereits abgelehnt!");
			return redirect(controllers.routes.GroupController.index());
		}
		
		// invitation?
		if(groupAccount != null && groupAccount.linkType.equals(LinkType.invite)){
			groupAccount.linkType = LinkType.establish;
			groupAccount.update();
			
			flash("success", "'" + group.title + "' erfolgreich beigetreten!");
			return redirect(controllers.routes.GroupController.index());
		}
		
		else if(group.groupType.equals(GroupType.open)){
			groupAccount = new GroupAccount(account, group, LinkType.establish);
			groupAccount.create();
			flash("success", "'" + group.title + "' erfolgreich beigetreten!");
			return redirect(controllers.routes.GroupController.view(id,  PAGE));
		}
		
		else if(group.groupType.equals(GroupType.close)){
			groupAccount = new GroupAccount(account, group, LinkType.request);
			groupAccount.create();
			Notification.newNotification(NotificationType.GROUP_NEW_REQUEST, group.id, group.owner);
			flash("success", "Deine Anfrage wurde erfolgreich übermittelt!");
			return redirect(controllers.routes.GroupController.index());
		}
		
		else if(group.groupType.equals(GroupType.course)){
			return redirect(controllers.routes.GroupController.token(id));
		}
						
		return redirect(controllers.routes.GroupController.index());
	}
		
	public static Result removeMember(long groupId, long accountId){
		Account account = Account.findById(accountId);
		Group group = Group.findById(groupId);
		GroupAccount groupAccount = GroupAccount.find(account, group);
		
		Call defaultRedirect = controllers.routes.GroupController.index();
		
		if(!Secured.removeGroupMember(group, account)) {
			return redirect(controllers.routes.GroupController.index());
		}
		
		if(groupAccount != null){
			groupAccount.delete();
			if(account.equals(Component.currentAccount())){
				flash("info", "Gruppe erfolgreich verlassen!");
			} else {
				flash("info", "Mitglied erfolgreich entfernt!");
				defaultRedirect = controllers.routes.GroupController.edit(groupId);
			}
			if(groupAccount.linkType.equals(LinkType.request)){
				flash("info", "Anfrage zurückgezogen!");			
			}
			if(groupAccount.linkType.equals(LinkType.reject)){
				flash("info", "Anfrage gelöscht!");			
			}
		} else {
			flash("info", "Das geht leider nicht :(");
		}
		return redirect(defaultRedirect);
	}
	
	public static Result acceptRequest(long groupId, long accountId){
		Account account = Account.findById(accountId);
		Group group = Group.findById(groupId);
		if(account != null && group != null && Secured.isOwnerOfGroup(group, Component.currentAccount())){
			GroupAccount groupAccount = GroupAccount.find(account, group);
			if(groupAccount != null){
				groupAccount.linkType = LinkType.establish;
				groupAccount.update();
			}
		}
		Notification.newNotification(NotificationType.GROUP_REQUEST_SUCCESS, groupId, account);
		return redirect(controllers.routes.GroupController.index());
	}
	
	public static Result declineRequest(long groupId, long accountId){
		Account account = Account.findById(accountId);
		Group group = Group.findById(groupId);
		if(account != null && group != null && Secured.isOwnerOfGroup(group, Component.currentAccount())){
			GroupAccount groupAccount = GroupAccount.find(account, group);
			if(groupAccount != null){
				groupAccount.linkType = LinkType.reject;
			}
		}
		Notification.newNotification(NotificationType.GROUP_REQUEST_DECLINE, groupId, account);
		return redirect(controllers.routes.GroupController.index());
	}
	
	public static Result invite(long groupId) {
		Group group = Group.findById(groupId);
		Navigation.set(Level.GROUPS, "Freunde einladen", group.title, controllers.routes.GroupController.view(group.id, PAGE));
		return ok(invite.render(group, Friendship.friendsToInvite(Component.currentAccount(), group), GroupAccount.findAccountsByGroup(group, LinkType.invite)));
	}
	
	public static Result inviteMember(long groupId) {
		Group group = Group.findById(groupId);
		Account currentUser = Component.currentAccount();
		
		if(Secured.inviteMember(group)) {
			DynamicForm form = Form.form().bindFromRequest();
			Collection<String> inviteList = form.data().values();	
			for (String accountId : inviteList) {
				try {
					Account account = Account.findById(Long.parseLong(accountId));
					GroupAccount groupAccount = GroupAccount.find(account, group);
					if(!Secured.isMemberOfGroup(group, account) && Friendship.alreadyFriendly(currentUser, account) && groupAccount == null) {
						new GroupAccount(account, group, LinkType.invite).create();
						Notification.newNotification(NotificationType.GROUP_INVITATION, group.id, account);
					}
				} catch (Exception e) {
					flash("error","Etwas ist schief gelaufen.");
					return redirect(controllers.routes.GroupController.invite(groupId));
				}
		    }
		}
		
		flash("success", "Einladung erfolgreich verschickt!");
		return redirect(controllers.routes.GroupController.view(groupId, PAGE));
	}
	
	public static Result acceptInvitation(long groupId, long accountId){
		Group group = Group.findById(groupId);
		Account account = Account.findById(accountId);
		GroupAccount groupAccount = GroupAccount.find(account,group);
		
		if(groupAccount != null && Secured.acceptInvitation(groupAccount) ){
			join(group.id);
			
		}
		
		return redirect(controllers.routes.GroupController.view(groupId, PAGE));
	}
	
	public static Result declineInvitation(long groupId, long accountId){
		Group group = Group.findById(groupId);
		Account account = Account.findById(accountId);
		GroupAccount groupAccount = GroupAccount.find(account,group);
		
		if(groupAccount != null && Secured.acceptInvitation(groupAccount) ){
			groupAccount.delete();
		}
		
		flash("success", "Einladung abgelehnt!");
		return redirect(controllers.routes.GroupController.index());
	}
}
