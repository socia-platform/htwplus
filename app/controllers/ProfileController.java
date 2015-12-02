package controllers;

import java.util.List;

import com.typesafe.config.ConfigFactory;
import models.*;
import models.enums.AccountRole;
import models.base.FileOperationException;
import models.base.ValidationException;
import models.enums.EmailNotifications;
import models.enums.LinkType;
import play.Logger;
import models.services.AvatarService;
import play.Play;
import play.data.Form;
import play.db.jpa.JPA;
import play.mvc.Call;
import play.mvc.Http.MultipartFormData;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Profile.*;
import controllers.Navigation.Level;
import play.Logger;
import play.libs.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import views.html.Profile.snippets.streamRaw;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Transactional
@Security.Authenticated(Secured.class)
public class ProfileController extends BaseController {

	static Form<Account> accountForm = Form.form(Account.class);
	static Form<Post> postForm = Form.form(Post.class);
    static Form<Login> loginForm = Form.form(Login.class);
	static final int LIMIT = Integer.parseInt(Play.application().configuration().getString("htwplus.post.limit"));

	public Result me() {
		Navigation.set(Level.PROFILE,"Ich");
		Account account = Component.currentAccount();
		if (account == null) {
			flash("info", "Diese Person gibt es nicht.");
			return redirect(controllers.routes.Application.index());
		} else {
			String avatar = Form.form().bindFromRequest().get("avatar");
			if(avatar != null && avatar.equals("success")){
				flash("success", "Dein Profilbild wurde erfolgreich gespeichert.");
			}
			return ok(index.render(account, postForm));
			// return ok(index.render(account));
		}
	}

	public Result view(final Long id) {
		Account account = Account.findById(id);

		if (account == null || account.role == AccountRole.DUMMY) {
			flash("info", "Diese Person gibt es nicht.");
			return redirect(controllers.routes.Application.index());
		} else {
			if(Secured.isFriend(account)) {
				Navigation.set(Level.FRIENDS, "Profil", account.name, controllers.routes.ProfileController.view(account.id));
			} else {
				Navigation.set(Level.USER, "Profil", account.name, controllers.routes.ProfileController.view(account.id));
			}
	
			return ok(index.render(account, postForm));
			// return ok(index.render(account));
		}
	}

    @Transactional
	public Result stream(Long accountId, int page, boolean raw) {
		Account account = Account.findById(accountId);
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
		if (Friendship.alreadyFriendly(Component.currentAccount(), account)
				|| currentUser.equals(account) || Secured.isAdmin()) {
            if(raw) {
                return ok(streamRaw.render(account, Post.getFriendStream(account, LIMIT, page),
                        postForm, Post.countFriendStream(account), LIMIT, page));
            } else {
                return ok(stream.render(account, Post.getFriendStream(account, LIMIT, page),
                        postForm, Post.countFriendStream(account), LIMIT, page));
            }
		}
		// case for visitors
		flash("info", "Du kannst nur den Stream deiner Freunde betrachten!");
		return redirect(controllers.routes.ProfileController.view(accountId));
	}

	public Result convert(Long id) {
		Account account = Account.findById(id);

		if (account == null) {
			flash("info", "Diese Person gibt es nicht.");
			return redirect(controllers.routes.Application.index());
		}
		// Check Access
		if(!Secured.isAdmin()) {
			return redirect(controllers.routes.Application.index());
		}

		Navigation.set(Level.PROFILE, "Konvertieren");
		return ok(convert.render(account, accountForm.fill(account)));
	}

	public Result saveConvert(Long id) {
		// Get regarding Object
		Account account = Account.findById(id);
		if (account == null) {
			flash("info", "Diese Person gibt es nicht.");
			return redirect(controllers.routes.Application.index());
		}

		// Create error switch
		Boolean error = false;

		// Check Access
		if(!Secured.isAdmin()) {
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
		if(filledForm.hasErrors()) {
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
			account.update();
			flash("success", "Account erfolgreich konvertiert.");
		}
		return redirect(controllers.routes.ProfileController.view(id));
	}

	public Result edit(Long id) {
		Account account = Account.findById(id);
		if (account == null) {
			flash("info", "Diese Person gibt es nicht.");
			return redirect(controllers.routes.Application.index());
		}
		
		// Check Access
		if (!Secured.editAccount(account)) {
			return redirect(controllers.routes.Application.index());
		}

        Navigation.set(Level.PROFILE, "Editieren");
		return ok(edit.render(account, accountForm.fill(account), loginForm));
	}

	public Result update(Long id) {
		// Get regarding Object
		Account account = Account.findById(id);
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
		Account exisitingAccount = Account.findByEmail(filledForm.field("email").value());
		if (exisitingAccount != null && !exisitingAccount.equals(account)) {
			filledForm.reject("email", "Diese Mail wird bereits verwendet!");
			return badRequest(edit.render(account, filledForm, loginForm));
		}

		// Perform JPA Validation
		if (filledForm.hasErrors()) {
			return badRequest(edit.render(account, filledForm, loginForm));
		} else {

			// Fill an and update the model manually 
			// because the its just a partial form
			if (!filledForm.field("email").value().isEmpty()) {
				account.email = filledForm.field("email").value();
			} else {
				account.email = null;
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

            if (filledForm.data().containsKey("emailNotifications")) {
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
				studycourse = Studycourse.findById(studycourseId);
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

			account.update();
		
			flash("success", "Profil erfolgreich gespeichert.");
			return redirect(controllers.routes.ProfileController.me());
		}
	}

    public Result groups(Long id) {
        Account account = Account.findById(id);

        if(Secured.isFriend(account)) {
            Navigation.set(Level.FRIENDS, "Gruppen & Kurse", account.name, controllers.routes.ProfileController.view(account.id));
        } else {
            if(Secured.isMe(id)) {
                Navigation.set(Level.PROFILE, "Gruppen & Kurse");
            } else {
                Navigation.set(Level.USER, "Gruppen & Kurse", account.name, controllers.routes.ProfileController.view(account.id));
            }
        }

        return ok(groups.render(account, GroupAccount.findGroupsEstablished(account),GroupAccount.findCoursesEstablished(account)));
    }

    @Transactional
    public Result deleteProfile(Long accountId) {
        Account current = Account.findById(accountId);

        if(!Secured.deleteAccount(current)) {
            flash("error", Messages.get("profile.delete.nopermission"));
            return redirect(controllers.routes.Application.index());
        }

        // Check Password //
        Form<Login> filledForm = loginForm.bindFromRequest();
        String entered = filledForm.field("password").value();
        if(entered == null || entered.length() == 0) {
            flash("error", Messages.get("profile.delete.nopassword"));
            return redirect(controllers.routes.ProfileController.update(current.id));
        } else if(!AccountController.checkPassword(accountId, entered)) {
            return redirect(controllers.routes.ProfileController.update(current.id));
        }

        // ACTUAL DELETION //
        Logger.info("Deleting Account[#"+current.id+"]...");
        current.delete();

        // override logout message
        Call logoutResult = controllers.routes.AccountController.logout();
        flash("success", Messages.get("profile.delete.success"));
        return redirect(logoutResult);
    }

	/**
	 * Handles the upload of the temporary avatar image
	 *
	 * @param id
	 * @return
	 */
	public Result createTempAvatar(Long id) {

		Account account = Account.findById(id);

		if(account == null){
			return notFound();
		}

		ObjectNode result = Json.newObject();
		MultipartFormData body = request().body().asMultipartFormData();
		
		if(body == null) {
			result.put("error", "No file attached");
			return badRequest(result);
		}
		
		MultipartFormData.FilePart avatar = body.getFile("avatarimage");

		if(avatar == null) {
			result.put("error", "No file with key 'avatarimage'");
			return badRequest(result);
		}

		try {
			account.setTempAvatar(avatar);
		} catch (ValidationException e) {
			result.put("error", e.getMessage());
			return badRequest(result);
		}
		
		result.put("success", controllers.routes.ProfileController.getTempAvatar(id).toString());
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
		Account account = Account.findById(id);

		if(account == null){
			return notFound();
		}

		if (!Secured.editAccount(account)) {
			result.put("error", "Not allowed.");
			return forbidden(result);
		}

		File tempAvatar = account.getTempAvatar();
		if(tempAvatar != null){
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
		
		Account account = Account.findById(id);

		if(account == null){
			return notFound();
		}

		if (!Secured.editAccount(account)) {
			result.put("error", "Not allowed.");
			return forbidden(result);
		}
		
		Form<Account.AvatarForm> form = Form.form(Account.AvatarForm.class).bindFromRequest();

		if(form.hasErrors()){
			result.put("error", form.errorsAsJson());
			return badRequest(result);
		}

		try {
			account.saveAvatar(form.get());
            account.indexAccount();
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
	 * @param id User ID
	 * @param size Size - Possible values: "small", "medium", "large"
	 * @return
	 */
	public Result getAvatar(long id, String size){
		Account account = Account.findById(id);
		if(account != null){
			File avatar;
			switch (size) {
				case "small":
					avatar = account.getAvatar(Account.AVATAR_SIZE.SMALL);
					break;
				case "medium":
					avatar = account.getAvatar(Account.AVATAR_SIZE.MEDIUM);
					break;
				case "large":
					avatar = account.getAvatar(Account.AVATAR_SIZE.LARGE);
					break;
				default:
					avatar = account.getAvatar(Account.AVATAR_SIZE.SMALL);
			}
			response().setHeader("Content-disposition","inline");
			if(avatar != null){
				return ok(avatar);
			} else {
				return notFound();
			}
		} else {
			return notFound();
		}
	}


}
