package controllers;

import static play.data.Form.form;

import java.util.Random;

import org.apache.commons.io.FileUtils;

import models.Account;
import models.enums.AccountRole;
import play.Logger;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Security;
import play.mvc.Result;
import play.mvc.With;
import views.html.Admin.*;

@Security.Authenticated(Secured.class)
// Action performs the authentication
@With(AdminAction.class)
public class AdminController extends BaseController {

	static Form<Account> accountForm = form(Account.class);
	
	public static Result index(){
		return ok(index.render());
	}
	
	public static Result createAccountForm(){
		return ok(createAccount.render(accountForm));
	}
	
	@Transactional
	public static Result createAccount() {
		Form<Account> filledForm = accountForm.bindFromRequest();
		Logger.info(filledForm.errors().toString());
		
		filledForm.errors().remove("role");

		if(filledForm.data().get("email").isEmpty()) {
			filledForm.reject("email", "Bitte gib hier etwas ein!");
		}
		
		if (!(Account.findByEmail(filledForm.data().get("email")) == null)) {
			filledForm.reject("email", "Diese Email-Adresse wird bereits verwendet!");
		}
		
		if (!filledForm.data().get("password").equals(filledForm.data().get("repeatPassword"))) {
			filledForm.reject("repeatPassword", "Passwörter stimmen nicht überein");
		}
		
		if (filledForm.data().get("password").length() < 6) {
			filledForm.reject("password", "Das Passwort muss mindestens 6 Zeichen haben.");
		}
		
		if(filledForm.hasErrors()) {
			return badRequest(createAccount.render(filledForm));
		}
		
		Account a = new Account();
		a.firstname = filledForm.data().get("firstname");
		a.lastname = filledForm.data().get("lastname");
		a.email = filledForm.data().get("email");
		a.password = Component.md5(filledForm.data().get("password"));
		a.avatar = "a1";
		a.role = AccountRole.values()[Integer.parseInt(filledForm.data().get("role"))];
		a.create();
		
		flash("success", "User angelegt");
		return ok(createAccount.render(accountForm));
		
	}
	
	public static Result viewMediaTemp() {
		//https://issues.apache.org/jira/browse/IO-373
		//String size = FileUtils.byteCountToDisplaySize(MediaController.sizeTemp());
		
		long bytes = MediaController.sizeTemp();
		String size = (bytes > 0) ? MediaController.bytesToString(bytes, false) : "keine Daten vorhanden";
		return ok(mediaTemp.render(size));
	}
	
	public static Result cleanMediaTemp(){
		MediaController.cleanUpTemp();
		flash("success", "Media Temp directory was cleaned.");
		return viewMediaTemp();
	}
	
	public static Result listAccounts(){
		return ok(listAccounts.render(Account.all()));
	}
	
	
}
	