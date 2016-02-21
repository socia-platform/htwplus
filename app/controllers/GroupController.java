package controllers;

import java.util.Collections;
import java.util.List;

import controllers.Navigation.Level;
import managers.*;
import models.*;
import models.enums.GroupType;
import models.enums.LinkType;
import models.services.NotificationService;
import play.Logger;
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
import java.util.List;


@Transactional
@Security.Authenticated(Secured.class)
public class GroupController extends BaseController {

    @Inject
    GroupManager groupManager;

    @Inject
    GroupAccountManager groupAccountManager;

    @Inject
    MediaManager mediaManager;

    @Inject
    FriendshipManager friendshipManager;

    @Inject
    PostManager postManager;

    @Inject
    AccountManager accountManager;

    @Inject
    FolderManager folderManager;

    static Form<Group> groupForm = Form.form(Group.class);
    static Form<Folder> folderForm = Form.form(Folder.class);
    static Form<Post> postForm = Form.form(Post.class);
    static final int LIMIT = Integer.parseInt(Play.application().configuration().getString("htwplus.post.limit"));
    static final int PAGE = 1;

    public Result index() {
        Navigation.set(Level.GROUPS, "Übersicht");
        Account account = Component.currentAccount();
        List<GroupAccount> groupRequests = groupAccountManager.findRequests(account);
        List<Group> groupAccounts = groupAccountManager.findGroupsEstablished(account);
        List<Group> courseAccounts = groupAccountManager.findCoursesEstablished(account);
        return ok(index.render(groupAccounts, courseAccounts, groupRequests, groupForm));
    }

    @Transactional(readOnly = true)
    public Result view(Long id) {
        Group group = groupManager.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }
        if (Secured.viewGroup(group)) {
            return redirect(controllers.routes.GroupController.stream(group.id, PAGE, false));
        }
        Navigation.set(Level.GROUPS, "Info", group.title, controllers.routes.GroupController.view(group.id));

        return ok(view.render(group));
    }

    @Transactional(readOnly = true)
    public Result stream(Long id, int page, boolean raw) {
        Group group = groupManager.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }
        if (!Secured.viewGroup(group)) {
            return redirect(controllers.routes.GroupController.view(group.id));
        }

        Navigation.set(Level.GROUPS, "Newsstream", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        List<Post> posts = postManager.getPostsForGroup(group, LIMIT, page);

        if (raw) {
            return ok(streamRaw.render(group, posts, postForm, postManager.countPostsForGroup(group), LIMIT, page));
        } else {
            return ok(stream.render(group, posts, postForm, postManager.countPostsForGroup(group), LIMIT, page));
        }
    }

    @Transactional(readOnly = true)
    public Result media(Long id, Long folderId) {
        Form<Media> mediaForm = Form.form(Media.class);
        Group group = groupManager.findById(id);
        Folder folder;

        if(folderId != 0) {
            folder = folderManager.findById(folderId);
        } else {
            folder = group.mediaFolder;
        }

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }
        if (!Secured.viewGroup(group)) {
            return redirect(controllers.routes.GroupController.view(id));
        }

        Navigation.set(Level.GROUPS, "Media", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        List<Media> mediaSet = folder.files;
        List<Folder> folderList = folder.folders;
        List<Folder> navigationFolder = folder.findAncestors(folder);
        Collections.reverse(navigationFolder);
        // hacky, but prevents accessing MediaController from view. use dto instead
        for (Media media : mediaSet) {
            media.sizeInByte = mediaManager.bytesToString(media.size, false);
        }
        return ok(media.render(group, mediaForm, mediaSet, folderList, folder, navigationFolder, folderForm));

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

            groupManager.createWithGroupAccount(group, Component.currentAccount());
            flash("success", successMsg + " erstellt!");
            return redirect(controllers.routes.GroupController.stream(group.id, PAGE, false));
        }
    }

    @Transactional
    public Result edit(Long id) {
        Group group = groupManager.findById(id);

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
        Group group = groupManager.findById(groupId);

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
        groupManager.update(group);
        flash("success", "'" + group.title + "' erfolgreich bearbeitet!");
        return redirect(controllers.routes.GroupController.stream(groupId, PAGE, false));

    }

    @Transactional
    public Result delete(Long id) {
        Group group = groupManager.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        if (Secured.deleteGroup(group)) {
            groupManager.delete(group);
            flash("info", "'" + group.title + "' wurde erfolgreich gelöscht!");
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
        }
        return redirect(controllers.routes.GroupController.index());
    }

    public Result token(Long groupId) {
        Group group = groupManager.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Navigation.set(Level.GROUPS, "Token eingeben", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        return ok(token.render(group, groupForm));
    }

    public Result validateToken(Long groupId) {
        Group group = groupManager.findById(groupId);

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
            groupAccountManager.create(new GroupAccount(group, account, LinkType.establish));
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
        Group group = groupManager.findById(id);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        if (Secured.isMemberOfGroup(group, account)) {
            flash("error", "Du bist bereits Mitglied dieser Gruppe!");
            return redirect(controllers.routes.GroupController.stream(id, PAGE, false));
        }

        // is already requested?
        GroupAccount groupAccount = groupAccountManager.find(account, group);
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
            groupAccountManager.update(groupAccount);

            flash("success", "'" + group.title + "' erfolgreich beigetreten!");
            return redirect(controllers.routes.GroupController.index());
        } else if (group.groupType.equals(GroupType.open)) {
            groupAccountManager.create(new GroupAccount(group, account, LinkType.establish));
            flash("success", "'" + group.title + "' erfolgreich beigetreten!");
            return redirect(controllers.routes.GroupController.stream(id, PAGE, false));
        } else if (group.groupType.equals(GroupType.close)) {
            groupAccountManager.create(new GroupAccount(group, account, LinkType.request));
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
        Account account = accountManager.findById(accountId);
        Group group = groupManager.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        GroupAccount groupAccount = groupAccountManager.find(account, group);

        Call defaultRedirect = controllers.routes.GroupController.index();

        if (!Secured.removeGroupMember(group, account)) {
            return redirect(controllers.routes.GroupController.index());
        }

        if (groupAccount != null) {
            groupAccountManager.delete(groupAccount);
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
        Account account = accountManager.findById(accountId);
        Group group = groupManager.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        if (account != null && Secured.isOwnerOfGroup(group, Component.currentAccount())) {
            GroupAccount groupAccount = groupAccountManager.find(account, group);
            if (groupAccount != null) {
                groupAccount.linkType = LinkType.establish;
                groupAccountManager.update(groupAccount);
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
        Account account = accountManager.findById(accountId);
        Group group = groupManager.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        if (account != null && Secured.isOwnerOfGroup(group, Component.currentAccount())) {
            GroupAccount groupAccount = groupAccountManager.find(account, group);
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
        Group group = groupManager.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Navigation.set(Level.GROUPS, "Kontakte einladen", group.title, controllers.routes.GroupController.stream(group.id, PAGE, false));
        return ok(invite.render(group, friendshipManager.friendsToInvite(Component.currentAccount(), group), GroupAccountManager.findAccountsByGroup(group, LinkType.invite)));
    }

    @Transactional
    public Result inviteMember(long groupId) {
        Group group = groupManager.findById(groupId);

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
                    Account inviteAccount = accountManager.findById(Long.parseLong(accountId));
                    GroupAccount groupAccount = groupAccountManager.find(inviteAccount, group);

                    // Create group account link to inviteAccount and add to notification recipient list
                    // if the inviteAccount is not already member, the sender and recipients are friends
                    // and the group account link is not already set up.
                    if (!Secured.isMemberOfGroup(group, inviteAccount) && FriendshipManager.alreadyFriendly(currentUser, inviteAccount) && groupAccount == null) {
                        groupAccountManager.create(new GroupAccount(group, inviteAccount, LinkType.invite));

                        // add inviteAccount to temporaryRecipients list for notifications later
                        group.addTemporaryRecipient(inviteAccount);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    flash("error", "Etwas ist schief gelaufen.");
                    return redirect(controllers.routes.GroupController.invite(groupId));
                }
            }

            group.temporarySender = currentUser;
            NotificationService.getInstance().createNotification(group, Group.GROUP_INVITATION);
        }

        flash("success", Messages.get("group.invite_invited"));
        return redirect(controllers.routes.GroupController.stream(groupId, PAGE, false));
    }

    public Result acceptInvitation(long groupId, long accountId) {
        Group group = groupManager.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Account account = accountManager.findById(accountId);
        GroupAccount groupAccount = groupAccountManager.find(account, group);

        if (groupAccount != null && Secured.acceptInvitation(groupAccount)) {
            join(group.id);

        }

        return redirect(controllers.routes.GroupController.stream(groupId, PAGE, false));
    }

    public Result declineInvitation(long groupId, long accountId) {
        Group group = groupManager.findById(groupId);

        if (group == null) {
            flash("error", Messages.get("group.group_not_found"));
            return redirect(controllers.routes.GroupController.index());
        }

        Account account = accountManager.findById(accountId);
        GroupAccount groupAccount = groupAccountManager.find(account, group);

        if (groupAccount != null && Secured.acceptInvitation(groupAccount)) {
            groupAccountManager.delete(groupAccount);
        }

        flash("success", "Einladung abgelehnt!");
        return redirect(controllers.routes.GroupController.index());
    }

    public Result createFolder(Long folderId) {
        Folder parentFolder = folderManager.findById(folderId);
        Group group = folderManager.findRoot(parentFolder).group;
        Folder folder = null;

        Form<Folder> filledForm = folderForm.bindFromRequest();

        if(filledForm.hasErrors()) {
            if(filledForm.data().get("name").isEmpty()) {
                flash("error", "Bitte einen Ordnernamen angeben.");
                return redirect(routes.GroupController.media(group.id, folderId));
            }
        }
        if(Secured.isMemberOfGroup(group, Component.currentAccount())) {
            folder = new Folder(filledForm.data().get("name"), Component.currentAccount(), parentFolder, null, null);
            folderManager.create(folder);
            return redirect(routes.GroupController.media(group.id, folder.id));
        }
        flash("error", Messages.get("post.join_group_first"));
        return redirect(routes.GroupController.media(group.id, folderId));
    }

    public Result deleteFolder(Long folderId) {
        Folder folder = folderManager.findById(folderId);
        Long groupId = folderManager.findRoot(folder).group.id;

        Call defaultRedirect = controllers.routes.GroupController.media(groupId, folderId);

        if(folder != null && Secured.deleteFolder(folder)) {
            if(folder.parent == null) {
                defaultRedirect = routes.GroupController.view(groupId);
            } else {
                defaultRedirect = routes.GroupController.media(groupId, folder.parent.id);
            }
            folderManager.delete(folder);
        }
        return redirect(defaultRedirect);
    }
}
