package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.Navigation.Level;
import managers.*;
import models.*;
import models.base.FileOperationException;
import models.base.ValidationException;
import models.enums.AccountRole;
import models.enums.AvatarSize;
import models.enums.EmailNotifications;
import play.Configuration;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Call;
import play.mvc.Http.MultipartFormData;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Profile.*;
import views.html.Profile.snippets.streamRaw;

import javax.inject.Inject;
import java.io.File;
import java.util.Collections;
import java.util.List;

@Transactional
@Security.Authenticated(Secured.class)
public class ProfileController extends BaseController {

    AccountManager accountManager;
    GroupAccountManager groupAccountManager;
    PostManager postManager;
    AccountController accountController;
    StudycourseManager studycourseManager;
    AvatarManager avatarManager;
    MediaManager mediaManager;
    Configuration configuration;
    FormFactory formFactory;

    @Inject
    public ProfileController(AccountManager accountManager,
            GroupAccountManager groupAccountManager,
            PostManager postManager,
            AccountController accountController,
            StudycourseManager studycourseManager,
            AvatarManager avatarManager,
            MediaManager mediaManager,
            Configuration configuration,FormFactory formFactory) {
        this.accountManager = accountManager;
        this.groupAccountManager = groupAccountManager;
        this.postManager = postManager;
        this.accountController = accountController;
        this.studycourseManager = studycourseManager;
        this.avatarManager = avatarManager;
        this.mediaManager = mediaManager;
        this.configuration = configuration;
        this.formFactory = formFactory;

    }
    Form<Account> accountForm = formFactory.form(Account.class);
    Form<Post> postForm = formFactory.form(Post.class);
    Form<Login> loginForm = formFactory.form(Login.class);
	final int LIMIT = Integer.parseInt(configuration.getString("htwplus.post.limit"));
	static final int PAGE = 1;

    public Result me() {
        Navigation.set(Level.PROFILE, "Ich");
        Account account = Component.currentAccount();
        if (account == null) {
            flash("info", "Diese Person gibt es nicht.");
            return redirect(controllers.routes.Application.index());
        } else {
            return ok(index.render(account, postForm));
        }
    }

    public Result view(final Long id) {
        Account account = accountManager.findById(id);

		if (account == null || account.role == AccountRole.DUMMY) {
			flash("info", "Diese Person gibt es nicht.");
			return redirect(controllers.routes.Application.index());
		} else {
			if(Secured.isFriend(account) || Component.currentAccount().equals(account)) {
				return redirect(routes.ProfileController.stream(account.id, PAGE, false));
			} else {
				Navigation.set(Level.USER, "Profil", account.name, controllers.routes.ProfileController.view(account.id));
			}
	
			return ok(index.render(account, postForm));
		}
	}

    @Transactional
    public Result stream(Long accountId, int page, boolean raw) {
        Account account = accountManager.findById(accountId);
        Account currentUser = Component.currentAccount();

        if (account == null || account.role == AccountRole.DUMMY) {
            flash("info", "Diese Person gibt es nicht.");
            return redirect(controllers.routes.Application.index());
        }

        if (currentUser.equals(account)) {
            Navigation.set(Level.PROFILE, "Newsstream");
        } else {
            Navigation.set(Level.FRIENDS, "Newsstream", account.name,
                    controllers.routes.ProfileController.view(account.id));
        }

        // case for friends and own profile
        if (FriendshipManager.alreadyFriendly(Component.currentAccount(), account)
                || currentUser.equals(account) || Secured.isAdmin()) {
            if (raw) {
                return ok(streamRaw.render(account, postManager.getFriendStream(account, LIMIT, page),
                        postForm, postManager.countFriendStream(account), LIMIT, page));
            } else {
                return ok(stream.render(account, postManager.getFriendStream(account, LIMIT, page),
                        postForm, postManager.countFriendStream(account), LIMIT, page));
            }
        }
        // case for visitors
        flash("info", "Du kannst nur den Stream deiner Kontakte betrachten!");
        return redirect(controllers.routes.ProfileController.view(accountId));
    }

    public Result convert(Long id) {
        Account account = accountManager.findById(id);

        if (account == null) {
            flash("info", "Diese Person gibt es nicht.");
            return redirect(controllers.routes.Application.index());
        }
        // Check Access
        if (!Secured.isAdmin()) {
            return redirect(controllers.routes.Application.index());
        }

        Navigation.set(Level.PROFILE, "Konvertieren");
        return ok(convert.render(account, accountForm.fill(account)));
    }

    public Result saveConvert(Long id) {
        // Get regarding Object
        Account account = accountManager.findById(id);
        if (account == null) {
            flash("info", "Diese Person gibt es nicht.");
            return redirect(controllers.routes.Application.index());
        }

        // Create error switch
        Boolean error = false;

        // Check Access
        if (!Secured.isAdmin()) {
            return redirect(controllers.routes.Application.index());
        }

        // Get data from request
        Form<Account> filledForm = accountForm.bindFromRequest();


        // Remove all unnecessary fields
        filledForm.errors().remove("firstname");
        filledForm.errors().remove("lastname");

        // Store old and new password for validation
        String email = filledForm.field("email").value();
        String password = filledForm.field("password").value();
        String repeatPassword = filledForm.field("repeatPassword").value();

        // Perform JPA Validation
        if (filledForm.hasErrors()) {
            error = true;
        }

        if (email.isEmpty()) {
            filledForm.reject("email", "Für einen lokalen Account ist eine EMail nötig!");
            error = true;
        }
        if (password.length() < 6) {
            filledForm.reject("password", "Das Passwort muss mindestens 6 Zeichen haben.");
            error = true;
        }

        if (!password.equals(repeatPassword)) {
            filledForm.reject("repeatPassword", "Die Passwörter stimmen nicht überein.");
            error = true;
        }

        if (error) {
            return badRequest(convert.render(account, filledForm));
        } else {
            account.password = Component.md5(password);
            account.email = email;
            accountManager.update(account);
            flash("success", "Account erfolgreich konvertiert.");
        }
        return redirect(controllers.routes.ProfileController.view(id));
    }

    public Result edit(Long id) {
        Account account = accountManager.findById(id);
        if (account == null) {
            flash("info", "Diese Person gibt es nicht.");
            return Secured.nullRedirect(request());
        }

        // Check Access
        if (!Secured.editAccount(account)) {
            return Secured.nullRedirect(request());
        }

        Navigation.set(Level.PROFILE, "Editieren");
        return ok(edit.render(account, accountForm.fill(account), loginForm, studycourseManager.getAll()));
    }

    public Result update(Long id) {
        // Get regarding Object
        Account account = accountManager.findById(id);
        if (account == null) {
            flash("info", "Diese Person gibt es nicht.");
            return redirect(controllers.routes.Application.index());
        }

        // Check Access
        if (!Secured.editAccount(account)) {
            return redirect(controllers.routes.Application.index());
        }

        // Get the data from the request
        Form<Account> filledForm = accountForm.bindFromRequest();

        Navigation.set(Level.PROFILE, "Editieren");

        // Remove expected errors
        filledForm.errors().remove("password");
        filledForm.errors().remove("studycourse");
        filledForm.errors().remove("firstname");
        filledForm.errors().remove("lastname");

        // Custom Validations
        Account exisitingAccount = accountManager.findByEmail(filledForm.field("email").value());
        if (exisitingAccount != null && !exisitingAccount.equals(account)) {
            filledForm.reject("email", "Diese Mail wird bereits verwendet!");
            return badRequest(edit.render(account, filledForm, loginForm, studycourseManager.getAll()));
        }

        // Perform JPA Validation
        if (filledForm.hasErrors()) {
            return badRequest(edit.render(account, filledForm, loginForm, studycourseManager.getAll()));
        } else {

            // Fill an and update the model manually
            // because the its just a partial form
            if (!filledForm.field("email").value().isEmpty()) {
                account.email = filledForm.field("email").value();
            } else {
                account.email = null;
                account.emailNotifications = EmailNotifications.NONE;
                account.dailyEmailNotificationHour = null;
            }

            if (filledForm.data().containsKey("degree")) {
                if (filledForm.field("degree").value().equals("null")) {
                    account.degree = null;
                } else {
                    account.degree = filledForm.field("degree").value();
                }
            }

            if (filledForm.data().containsKey("semester")) {
                if (filledForm.field("semester").value().equals("0")) {
                    account.semester = null;
                } else {
                    account.semester = Integer.parseInt(filledForm.field("semester").value());
                }
            }

            if (filledForm.data().containsKey("emailNotifications") && (account.email != null && !account.email.isEmpty())) {
                account.emailNotifications = EmailNotifications.valueOf(filledForm.field("emailNotifications").value());
                if (account.emailNotifications.equals(EmailNotifications.COLLECTED_DAILY)
                        && filledForm.data().containsKey("dailyEmailNotificationHour")) {
                    account.dailyEmailNotificationHour = Integer.valueOf(
                            filledForm.field("dailyEmailNotificationHour").value()
                    );
                }
            }

            Long studycourseId = Long.parseLong(filledForm.field("studycourse").value());
            Studycourse studycourse;
            if (studycourseId != 0) {
                studycourse = studycourseManager.findById(studycourseId);
            } else {
                studycourse = null;
            }
            account.studycourse = studycourse;

            if (!filledForm.field("about").value().isEmpty()) {
                account.about = filledForm.field("about").value();
            } else {
                account.about = null;
            }

            if (!filledForm.field("homepage").value().isEmpty()) {
                account.homepage = filledForm.field("homepage").value();
            } else {
                account.homepage = null;
            }

            accountManager.update(account);

            flash("success", "Profil erfolgreich gespeichert.");
            return redirect(controllers.routes.ProfileController.me());
        }
    }

    public Result groups(Long id) {
        Account account = accountManager.findById(id);

        if (Secured.isFriend(account)) {
            Navigation.set(Level.FRIENDS, "Gruppen & Kurse", account.name, controllers.routes.ProfileController.view(account.id));
        } else {
            if (Secured.isMe(account)) {
                Navigation.set(Level.PROFILE, "Gruppen & Kurse");
            } else {
                Navigation.set(Level.USER, "Gruppen & Kurse", account.name, controllers.routes.ProfileController.view(account.id));
            }
        }

        return ok(groups.render(account, groupAccountManager.findGroupsEstablished(account), groupAccountManager.findCoursesEstablished(account)));
    }

    public Result files(Long accountId) {
        Account account = accountManager.findById(accountId);

        // check account
        if (account == null) {
            flash("info", Messages.get("Diese Person gibt es nicht."));
            return Secured.nullRedirect(request());
        }

        // unknown user
        if (Secured.isUnknown(account)) {
            flash("info", "Du kannst nur Dateien deiner Kontakte sehen.");
            return redirect(controllers.routes.ProfileController.view(account.id));
        }

        // set Navigation
        if (Secured.isFriend(account)) {
            Navigation.set(Level.FRIENDS, "Dateien", account.name, controllers.routes.ProfileController.view(account.id));
        } else {
            if (Secured.isMe(account)) {
                Navigation.set(Level.PROFILE, "Dateien");
            } else {
                Navigation.set(Level.USER, "Dateien", account.name, controllers.routes.ProfileController.view(account.id));
            }
        }

        // gather information
        Folder accountRootFolder = account.rootFolder;
        List<Media> fileList = accountRootFolder.files;
        List<Folder> folderList = accountRootFolder.folders;
        List<Folder> navigationFolder = accountRootFolder.findAncestors(accountRootFolder);
        Collections.reverse(navigationFolder);
        // hacky, but prevents accessing MediaController from view. use dto instead
        for (Media media : fileList) {
            media.sizeInByte = mediaManager.bytesToString(media.size, false);
        }

        return ok(files.render(account, fileList, folderList));
    }

    @Transactional
    public Result deleteProfile(Long accountId) {
        Account current = accountManager.findById(accountId);

        if (!Secured.deleteAccount(current)) {
            flash("error", Messages.get("profile.delete.nopermission"));
            return redirect(controllers.routes.Application.index());
        }

        // Check Password //
        Form<Login> filledForm = loginForm.bindFromRequest();
        String entered = filledForm.field("password").value();
        if (entered == null || entered.length() == 0) {
            flash("error", Messages.get("profile.delete.nopassword"));
            return redirect(controllers.routes.ProfileController.update(current.id));
        } else if (!accountController.checkPassword(accountId, entered)) {
            return redirect(controllers.routes.ProfileController.update(current.id));
        }

        // ACTUAL DELETION //
        Logger.info("Deleting Account[#" + current.id + "]...");
        accountManager.delete(current);

        // override logout message
        flash("success", Messages.get("profile.delete.success"));
        return redirect(controllers.routes.AccountController.logout());
    }

    /**
     * Handles the upload of the temporary avatar image
     *
     * @param id
     * @return
     */
    public Result createTempAvatar(Long id) {

        Account account = accountManager.findById(id);

        if (account == null) {
            return notFound();
        }

        ObjectNode result = Json.newObject();
        MultipartFormData body = request().body().asMultipartFormData();

        if (body == null) {
            result.put("error", "No file attached");
            return badRequest(result);
        }

        MultipartFormData.FilePart avatar = body.getFile("avatarimage");

        if (avatar == null) {
            result.put("error", "No file with key 'avatarimage'");
            return badRequest(result);
        }

        try {
            avatarManager.setTempAvatar(avatar, account.id);
        } catch (ValidationException e) {
            result.put("error", e.getMessage());
            return badRequest(result);
        }

        result.put("success", getTempAvatar(id).toString());
        return ok(result);
    }

    /**
     * Get the temporary avatar image
     *
     * @param id
     * @return
     */
    public Result getTempAvatar(Long id) {

        ObjectNode result = Json.newObject();
        Account account = accountManager.findById(id);

        if (account == null) {
            return notFound();
        }

        if (!Secured.editAccount(account)) {
            result.put("error", "Not allowed.");
            return forbidden(result);
        }

        File tempAvatar = avatarManager.getTempAvatar(account.id);
        if (tempAvatar != null) {
            return ok(tempAvatar);
        } else {
            return notFound();
        }
    }

    /**
     * Create the real avatar with the given dimensions
     *
     * @param id
     * @return
     */
    public Result createAvatar(long id) {
        ObjectNode result = Json.newObject();

        Account account = accountManager.findById(id);

        if (account == null) {
            return notFound();
        }

        if (!Secured.editAccount(account)) {
            result.put("error", "Not allowed.");
            return forbidden(result);
        }

        Form<Avatar> form = formFactory.form(Avatar.class).bindFromRequest();

        if (form.hasErrors()) {
            result.put("error", form.errorsAsJson());
            return badRequest(result);
        }

        try {
            accountManager.saveAvatar(form.get(), account);
            result.put("success", "saved");
            return ok(result);
        } catch (FileOperationException e) {
            result.put("error", "Unexpected Error while saving avatar.");
            return internalServerError(result);
        }
    }

    /**
     * Get the avatar of a user.
     *
     * @param id   User ID
     * @param size Size - Possible values: "small", "medium", "large"
     * @return
     */
    public Result getAvatar(long id, String size) {
        Account account = accountManager.findById(id);
        if (account != null) {
            File avatar;
            switch (size) {
                case "small":
                    avatar = avatarManager.getAvatar(AvatarSize.SMALL, account.id);
                    break;
                case "medium":
                    avatar = avatarManager.getAvatar(AvatarSize.MEDIUM, account.id);
                    break;
                case "large":
                    avatar = avatarManager.getAvatar(AvatarSize.LARGE, account.id);
                    break;
                default:
                    avatar = avatarManager.getAvatar(AvatarSize.SMALL, account.id);
            }
            response().setHeader("Content-disposition", "inline");
            if (avatar != null) {
                return ok(avatar);
            } else {
                return notFound();
            }
        } else {
            return notFound();
        }
    }

}
