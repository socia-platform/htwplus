package controllers;

import static play.data.Form.form;

import java.util.Random;

import models.Account;
import models.LDAPConnector;
import models.LDAPConnector.LDAPConnectorException;
import models.enums.AccountRole;
import models.Login;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Result;
import views.html.index;

@Transactional
public class AccountController extends BaseController {

	/**
	 * Defines a form wrapping the Account class.
	 */
	final static Form<Account> signupForm = form(Account.class);

	public static Result authenticate() {
		DynamicForm form = form().bindFromRequest();
		String username = form.field("email").value();
	
		if (username.contains("@")) {
			return defaultAuthenticate();
		} else if (username.length() == 0) {
			flash("error", "Also deine Matrikelnummer brauchen wir schon!");
			return badRequest(index.render());
		} else {
			return LDAPAuthenticate();
		}
	}

	private static Result LDAPAuthenticate() {
		Form<Login> form = form(Login.class).bindFromRequest();

		String username = form.field("email").value();
		String password = form.field("password").value();
		
		// Clean the username
		username = username.trim().toLowerCase();

		LDAPConnector ldap = new LDAPConnector();
		try {
			ldap.connect(username, password);
		} catch (LDAPConnectorException e) {
			flash("error", e.getMessage());
			Component.addToContext(Component.ContextIdent.loginForm, form);
			return badRequest(index.render());
		}

		Account account = Account.findByLoginName(ldap.getUsername());
		AccountRole role = AccountRole.STUDENT;
		if(ldap.getRole() != null) {
			role = ldap.getRole();
		}
		
		if (account == null) {
			account = new Account();
			Logger.info("New Account for " + ldap.getUsername()
					+ " will be created.");
			account.firstname = ldap.getFirstname();
			account.lastname = ldap.getLastname();
			account.loginname = ldap.getUsername();
			account.password = "LDAP - not needed";
			Random generator = new Random();
			account.avatar = "a" + generator.nextInt(10);
			account.role = role;
			account.create();
		} else {
			account.firstname = ldap.getFirstname();
			account.lastname = ldap.getLastname();
			account.role = role;
			account.update();
		}

		session().clear();
		session("id", account.id.toString());
		if(form.get().rememberMe != null){
			session("rememberMe", "1");
		}
		return redirect(routes.Application.index());
	}

	private static Result defaultAuthenticate() {
		Form<Login> loginForm = form(Login.class).bindFromRequest();
		if (loginForm.hasErrors()) {
			flash("error", loginForm.globalError().message());
			Component.addToContext(Component.ContextIdent.loginForm, loginForm);
			return badRequest(index.render());
		} else {
			session().clear();
			session("email", loginForm.get().email);
			session("id",
					Account.findByEmail(loginForm.get().email).id.toString());
			session("firstname",
					Account.findByEmail(loginForm.get().email).firstname);
			if(loginForm.get().rememberMe != null){
				session("rememberMe", "1");
			}
			
			return redirect(routes.Application.index());
		}
	}

	/**
	 * Logout and clean the session.
	 */
	public static Result logout() {
		session().clear();
		flash("success", "Du bist nun ausgeloggt");
		return redirect(routes.Application.index());
	}

}