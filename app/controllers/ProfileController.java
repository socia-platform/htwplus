package controllers;

import models.*;
import models.enums.EmailNotifications;
import play.Play;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import views.html.Profile.edit;
import views.html.Profile.editPassword;
import views.html.Profile.index;
import views.html.Profile.stream;
import controllers.Navigation.Level;

@Transactional
@Security.Authenticated(Secured.class)
public class ProfileController extends BaseController {

	static Form<Account> accountForm = Form.form(Account.class);
	static Form<Post> postForm = Form.form(Post.class);
	static final int LIMIT = Integer.parseInt(Play.application().configuration().getString("htwplus.post.limit"));

	public static Result me() {
		Navigation.set(Level.PROFILE,"Ich");
		Account account = Component.currentAccount();
		if (account == null) {
			flash("info", "Diese Person gibt es nicht.");
			return redirect(controllers.routes.Application.index());
		} else {
			return ok(index.render(account, postForm));
			// return ok(index.render(account));
		}
	}

	public static Result view(final Long id) {
		Account account = Account.findById(id);

		if (account == null) {
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
	public static Result stream(Long accountId, int page) {
		Account account = Account.findById(accountId);
		Account currentUser = Component.currentAccount();
		
		if (account == null) {
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
			return ok(stream.render(account, Post.getFriendStream(account, LIMIT, page),
					postForm,Post.countFriendStream(account), LIMIT, page));
		}
		// case for visitors
		flash("info", "Du kannst nur den Stream deiner Freunde betrachten!");
		return redirect(controllers.routes.ProfileController.view(accountId));
	}

	public static Result editPassword(Long id) {
		Account account = Account.findById(id);
		
		if (account == null) {
			flash("info", "Diese Person gibt es nicht.");
			return redirect(controllers.routes.Application.index());
		}
		// Check Access
		if(!Secured.editAccount(account)) {
			return redirect(controllers.routes.Application.index());
		}
		
		Navigation.set(Level.PROFILE, "Editieren");
		return ok(editPassword.render(account, accountForm.fill(account)));
	}

	public static Result updatePassword(Long id) {
		// Get regarding Object
		Account account = Account.findById(id);
		if (account == null) {
			flash("info", "Diese Person gibt es nicht.");
			return redirect(controllers.routes.Application.index());
		}
		
		// Create error switch
		Boolean error = false;
		
		// Check Access
		if(!Secured.editAccount(account)) {
			return redirect(controllers.routes.Application.index());
		}
		
		// Get data from request
		Form<Account> filledForm = accountForm.bindFromRequest();
		
		
		// Remove all unnecessary fields
		filledForm.errors().remove("firstname");
		filledForm.errors().remove("lastname");
		filledForm.errors().remove("email");

		// Store old and new password for validation
		String oldPassword = filledForm.field("oldPassword").value();
		String password = filledForm.field("password").value();
		String repeatPassword = filledForm.field("repeatPassword").value();
		
		// Perform JPA Validation
		if(filledForm.hasErrors()) {
			error = true;
		}
		
		// Custom Validations
		if (!oldPassword.isEmpty()) {
			if (!account.password.equals(Component.md5(oldPassword))) {
				filledForm.reject("oldPassword", "Dein altes Passwort ist nicht korrekt.");
				error = true;
			}
		} else {
			filledForm.reject("oldPassword","Bitte gebe Dein altes Passwort ein.");
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
			return badRequest(editPassword.render(account, filledForm));
		} else {
			account.password = Component.md5(password);
			account.update();
			flash("success", "Passwort erfolgreich geändert.");
		}
		return redirect(controllers.routes.ProfileController.me());
	}

	public static Result edit(Long id) {
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
		return ok(edit.render(account, accountForm.fill(account)));
	}

	public static Result update(Long id) {
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
			return badRequest(edit.render(account, filledForm));
		}
		
		// Perform JPA Validation
		if (filledForm.hasErrors()) {
			return badRequest(edit.render(account, filledForm));
		} else {

			// Fill an and update the model manually 
			// because the its just a partial form
			account.avatar = filledForm.field("avatar").value();
			
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
			account.update();
		
			flash("success", "Profil erfolgreich gespeichert.");
			return redirect(controllers.routes.ProfileController.me());
		}
	}
}
