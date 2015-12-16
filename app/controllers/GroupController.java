package controllers;

import java.util.List;

import controllers.Navigation.Level;
import daos.GroupDao;
import models.*;
import models.enums.GroupType;
import models.enums.LinkType;
import models.services.NotificationService;
import play.Play;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Group.*;
import views.html.Group.snippets.streamRaw;

import javax.inject.Inject;


@Transactional
@Security.Authenticated(Secured.class)
public class GroupController extends BaseController {

    @Inject
    MediaController mediaController;

    @Inject
    GroupAccount groupAccount;

    @Inject
    GroupDao groupDao;

    static Form<Group> groupForm = Form.form(Group.class);
    static Form<Post> postForm = Form.form(Post.class);
    static final int LIMIT = Integer.parseInt(Play.application().configuration().getString("htwplus.post.limit"));
    static final int PAGE = 1;

    public Result index() {
        Navigation.set(Level.GROUPS, "Übersicht");
        Account account = Component.currentAccount();
        List<GroupAccount> groupRequests = GroupAccount.findRequests(account);
        List<Group> groupAccounts = GroupAccount.findGroupsEstablished(account);
        List<Group> courseAccounts = GroupAccount.findCoursesEstablished(account);
        return ok(index.render(groupAccounts, courseAccounts, groupRequests, groupForm));
    }

    @Transactional(readOnly = true)
    public Result view(Long id) {
        Group group = Group.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }
        if (Secured.viewGroup(group)) {
            return redirect(routes.GroupController.stream(group.id, PAGE, false));
        }
        Navigation.set(Level.GROUPS, "Info", group.title, controllers.routes.GroupController.view(group.id));

        return ok(view.render(group));
    }

    @Transactional(readOnly = true)
    public Result stream(Long id, int page, boolean raw) {
        Group group = Group.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }
        if (!Secured.viewGroup(group)) {
            return redirect(routes.GroupController.view(group.id));
        }

        Navigation.set(Level.GROUPS, "Newsstream", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        List<Post> posts = Post.getPostsForGroup(group, LIMIT, page);

        if (raw) {
            return ok(streamRaw.render(group, posts, postForm, Post.countPostsForGroup(group), LIMIT, page));
        } else {
            return ok(stream.render(group, posts, postForm, Post.countPostsForGroup(group), LIMIT, page));
        }
    }

    @Transactional(readOnly = true)
    public Result media(Long id) {
        Form<Media> mediaForm = Form.form(Media.class);
        Group group = Group.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }
        if (!Secured.viewGroup(group)) {
            return redirect(routes.GroupController.view(id));
        }

        Navigation.set(Level.GROUPS, "Media", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        List<Media> mediaSet = group.media;
        // hacky, but prevents accessing MediaController from view. use dto instead
        for (Media media : mediaSet) {
            media.sizeInByte = mediaController.bytesToString(media.size, false);
        }
        return ok(media.render(group, mediaForm, mediaSet));

    }

    public Result create() {
        Navigation.set(Level.GROUPS, "Erstellen");
        return ok(create.render(groupForm));
    }

    public Result add() {
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
            } catch (NumberFormatException ex) {
                filledForm.reject("type", "Bitte eine Sichtbarkeit wählen!");
                return ok(create.render(filledForm));
            }

            String successMsg;
            switch (groupType) {

                case 0:
                    group.groupType = GroupType.open;
                    successMsg = "Öffentliche Gruppe";
                    break;

                case 1:
                    group.groupType = GroupType.close;
                    successMsg = "Geschlossene Gruppe";
                    break;

                case 2:
                    group.groupType = GroupType.course;
                    successMsg = "Kurs";
                    String token = filledForm.data().get("token");
                    if (!Group.validateToken(token)) {
                        filledForm.reject("token", "Bitte einen Token zwischen 4 und 45 Zeichen eingeben!");
                        return ok(create.render(filledForm));
                    }

                    if (!Secured.createCourse()) {
                        flash("error", "Du darfst leider keinen Kurs erstellen");
                        return badRequest(create.render(filledForm));
                    }
                    break;

                default:
                    filledForm.reject("Nicht möglich!");
                    return ok(create.render(filledForm));
            }

            groupDao.createWithGroupAccount(group);
            flash("success", successMsg + " erstellt!");
            return redirect(controllers.routes.GroupController.stream(group.id, PAGE, false));
        }
    }

    @Transactional
    public Result edit(Long id) {
        Group group = Group.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        // Check rights
        if (!Secured.editGroup(group)) {
            return redirect(controllers.routes.GroupController.view(id));
        }

        Navigation.set(Level.GROUPS, "Bearbeiten", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        Form<Group> groupForm = Form.form(Group.class).fill(group);
        groupForm.data().put("type", String.valueOf(group.groupType.ordinal()));
        return ok(edit.render(group, groupForm));

    }

    @Transactional
    public Result update(Long groupId) {
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Navigation.set(Level.GROUPS, "Bearbeiten", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));

        // Check rights
        if (!Secured.editGroup(group)) {
            return redirect(controllers.routes.GroupController.index());
        }

        Form<Group> filledForm = groupForm.bindFromRequest();
        int groupType = Integer.parseInt(filledForm.data().get("type"));
        String description = filledForm.data().get("description");

        switch (groupType) {
            case 0:
                group.groupType = GroupType.open;
                group.token = null;
                break;
            case 1:
                group.groupType = GroupType.close;
                group.token = null;
                break;
            case 2:
                group.groupType = GroupType.course;
                String token = filledForm.data().get("token");
                if (!Group.validateToken(token)) {
                    filledForm.reject("token", "Bitte einen Token zwischen 4 und 45 Zeichen eingeben!");
                    return ok(edit.render(group, filledForm));
                }
                if (!Secured.createCourse()) {
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
        return redirect(controllers.routes.GroupController.stream(groupId, PAGE, false));

    }

    @Transactional
    public Result delete(Long id) {
        Group group = Group.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        if (Secured.deleteGroup(group)) {
            group.delete();
            flash("info", "'" + group.title + "' wurde erfolgreich gelöscht!");
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
        }
        return redirect(controllers.routes.GroupController.index());
    }

    public Result token(Long groupId) {
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Navigation.set(Level.GROUPS, "Token eingeben", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        return ok(token.render(group, groupForm));
    }

    public Result validateToken(Long groupId) {
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        if (Secured.isMemberOfGroup(group, Component.currentAccount())) {
            flash("error", "Du bist bereits Mitglied dieser Gruppe!");
            return redirect(controllers.routes.GroupController.stream(groupId, PAGE, false));
        }

        Navigation.set(Level.GROUPS, "Token eingeben", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        Form<Group> filledForm = groupForm.bindFromRequest();
        String enteredToken = filledForm.data().get("token");

        if (enteredToken.equals(group.token)) {
            Account account = Component.currentAccount();

            groupAccount.account = account;
            groupAccount.group = group;
            groupAccount.linkType = LinkType.establish;
            groupAccount.create();
            flash("success", "Kurs erfolgreich beigetreten!");
            return redirect(controllers.routes.GroupController.stream(groupId, PAGE, false));
        } else {
            flash("error", "Hast du dich vielleicht vertippt? Der Token ist leider falsch.");
            return badRequest(token.render(group, filledForm));
        }
    }

    @Transactional
    public Result join(long id) {
        Account account = Component.currentAccount();
        Group group = Group.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        if (Secured.isMemberOfGroup(group, account)) {
            flash("error", "Du bist bereits Mitglied dieser Gruppe!");
            return redirect(controllers.routes.GroupController.stream(id, PAGE, false));
        }

        // is already requested?
        groupAccount = GroupAccount.find(account, group);
        if (groupAccount != null && groupAccount.linkType.equals(LinkType.request)) {
            flash("info", "Deine Beitrittsanfrage wurde bereits verschickt!");
            return redirect(controllers.routes.GroupController.index());
        }

        if (groupAccount != null && groupAccount.linkType.equals(LinkType.reject)) {
            flash("error", "Deine Beitrittsanfrage wurde bereits abgelehnt!");
            return redirect(controllers.routes.GroupController.index());
        }

        // invitation?
        if (groupAccount != null && groupAccount.linkType.equals(LinkType.invite)) {
            groupAccount.linkType = LinkType.establish;
            groupAccount.update();

            flash("success", "'" + group.title + "' erfolgreich beigetreten!");
            return redirect(controllers.routes.GroupController.index());
        } else if (group.groupType.equals(GroupType.open)) {
            groupAccount.account = account;
            groupAccount.group = group;
            groupAccount.linkType = LinkType.establish;
            groupAccount.create();
            flash("success", "'" + group.title + "' erfolgreich beigetreten!");
            return redirect(controllers.routes.GroupController.stream(id, PAGE, false));
        } else if (group.groupType.equals(GroupType.close)) {
            groupAccount.account = account;
            groupAccount.group = group;
            groupAccount.linkType = LinkType.request;
            groupAccount.create();
            group.temporarySender = account;
            NotificationService.getInstance().createNotification(group, Group.GROUP_NEW_REQUEST);
            flash("success", Messages.get("group.group_request_sent"));
            return redirect(controllers.routes.GroupController.index());
        } else if (group.groupType.equals(GroupType.course)) {
            return redirect(controllers.routes.GroupController.token(id));
        }

        return redirect(controllers.routes.GroupController.index());
    }

    public Result removeMember(long groupId, long accountId) {
        Account account = Account.findById(accountId);
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        GroupAccount groupAccount = GroupAccount.find(account, group);

        Call defaultRedirect = controllers.routes.GroupController.index();

        if (!Secured.removeGroupMember(group, account)) {
            return redirect(controllers.routes.GroupController.index());
        }

        if (groupAccount != null) {
            groupAccount.delete();
            if (account.equals(Component.currentAccount())) {
                flash("info", "Gruppe erfolgreich verlassen!");
            } else {
                flash("info", "Mitglied erfolgreich entfernt!");
                defaultRedirect = controllers.routes.GroupController.edit(groupId);
            }
            if (groupAccount.linkType.equals(LinkType.request)) {
                flash("info", "Anfrage zurückgezogen!");
            }
            if (groupAccount.linkType.equals(LinkType.reject)) {
                flash("info", "Anfrage gelöscht!");
            }
        } else {
            flash("info", "Das geht leider nicht :(");
        }
        return redirect(defaultRedirect);
    }

    /**
     * Accepts a group entry request.
     *
     * @param groupId   Group ID
     * @param accountId Account ID
     * @return Result
     */
    @Transactional
    public Result acceptRequest(long groupId, long accountId) {
        Account account = Account.findById(accountId);
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        if (account != null && Secured.isOwnerOfGroup(group, Component.currentAccount())) {
            GroupAccount groupAccount = GroupAccount.find(account, group);
            if (groupAccount != null) {
                groupAccount.linkType = LinkType.establish;
                groupAccount.update();
            }
        } else {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        group.temporarySender = group.owner;
        group.addTemporaryRecipient(account);
        NotificationService.getInstance().createNotification(group, Group.GROUP_REQUEST_SUCCESS);

        return redirect(controllers.routes.GroupController.index());
    }

    /**
     * Declines a group entry request.
     *
     * @param groupId   Group ID
     * @param accountId Account ID
     * @return Result
     */
    @Transactional
    public Result declineRequest(long groupId, long accountId) {
        Account account = Account.findById(accountId);
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        if (account != null && Secured.isOwnerOfGroup(group, Component.currentAccount())) {
            GroupAccount groupAccount = GroupAccount.find(account, group);
            if (groupAccount != null) {
                groupAccount.linkType = LinkType.reject;
            }
        }
        group.temporarySender = group.owner;
        group.addTemporaryRecipient(account);
        NotificationService.getInstance().createNotification(group, Group.GROUP_REQUEST_DECLINE);

        return redirect(controllers.routes.GroupController.index());
    }

    @Transactional
    public Result invite(long groupId) {
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Navigation.set(Level.GROUPS, "Freunde einladen", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        return ok(invite.render(group, Friendship.friendsToInvite(Component.currentAccount(), group), GroupAccount.findAccountsByGroup(group, LinkType.invite)));
    }

    @Transactional
    public Result inviteMember(long groupId) {
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Account currentUser = Component.currentAccount();

        if (Secured.inviteMember(group)) {
            // bind invite list to group
            DynamicForm form = Form.form().bindFromRequest();
            group.inviteList = form.data().values();

            // if no one is invited, abort
            if (group.inviteList.size() < 1) {
                flash("error", Messages.get("group.invite_no_invite"));
                return redirect(controllers.routes.GroupController.invite(groupId));
            }

            // create GroupAccount link for all invitations
            for (String accountId : group.inviteList) {
                try {
                    Account inviteAccount = Account.findById(Long.parseLong(accountId));
                    GroupAccount groupAccount = GroupAccount.find(inviteAccount, group);

                    // Create group account link to inviteAccount and add to notification recipient list
                    // if the inviteAccount is not already member, the sender and recipients are friends
                    // and the group account link is not already set up.
                    if (!Secured.isMemberOfGroup(group, inviteAccount) && Friendship.alreadyFriendly(currentUser, inviteAccount) && groupAccount == null) {
                        groupAccount.account = inviteAccount;
                        groupAccount.group = group;
                        groupAccount.linkType = LinkType.invite;
                        groupAccount.create();

                        // add inviteAccount to temporaryRecipients list for notifications later
                        group.addTemporaryRecipient(inviteAccount);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    flash("error", "Etwas ist schief gelaufen.");
                    return redirect(routes.GroupController.invite(groupId));
                }
            }

            group.temporarySender = currentUser;
            NotificationService.getInstance().createNotification(group, Group.GROUP_INVITATION);
        }

        flash("success", Messages.get("group.invite_invited"));
        return redirect(controllers.routes.GroupController.stream(groupId, PAGE, false));
    }

    public Result acceptInvitation(long groupId, long accountId) {
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Account account = Account.findById(accountId);
        GroupAccount groupAccount = GroupAccount.find(account, group);

        if (groupAccount != null && Secured.acceptInvitation(groupAccount)) {
            join(group.id);

        }

        return redirect(controllers.routes.GroupController.stream(groupId, PAGE, false));
    }

    public Result declineInvitation(long groupId, long accountId) {
        Group group = Group.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Account account = Account.findById(accountId);
        GroupAccount groupAccount = GroupAccount.find(account, group);

        if (groupAccount != null && Secured.acceptInvitation(groupAccount)) {
            groupAccount.delete();
        }

        flash("success", "Einladung abgelehnt!");
        return redirect(controllers.routes.GroupController.index());
    }
}
