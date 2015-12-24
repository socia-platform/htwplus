package controllers;

import com.google.inject.Inject;
import managers.AccountManager;
import models.Account;
import models.Login;
import models.enums.AccountRole;
import models.services.LdapService;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Result;
import views.html.landingpage;

import java.util.Random;

import static play.data.Form.form;

/**
 * Controller for authenticate purposes.
 */
@Transactional
public class AccountController extends BaseController {

    @Inject
    AccountManager accountManager;

    /**
     * Defines a form wrapping the Account class.
     */
    final static Form<Account> signupForm = form(Account.class);

    /**
     * Default authentication action.
     *
     * @return Result
     */
    public Result authenticate() {
        DynamicForm form = form().bindFromRequest();
        String username = form.field("email").value();

        // save originURL before clearing the session (it gets cleared in defaultAuthenticate() and LdapAuthenticate())
        String redirect = session().get("originURL");

        if (username.contains("@")) {
            return defaultAuthenticate(redirect);
        } else if (username.length() == 0) {
            flash("error", "Also deine Matrikelnummer brauchen wir schon!");
            return badRequest(landingpage.render());
        } else {
            return LdapAuthenticate(redirect);
        }
    }

    /**
     * LDAP authentication.
     *
     * @return Result
     */
    private Result LdapAuthenticate(final String redirect) {
        Form<Login> form = form(Login.class).bindFromRequest();
        String matriculationNumber = form.field("email").value();
        String password = form.field("password").value();
        String rememberMe = form.field("rememberMe").value();

        // Clean the username
        matriculationNumber = matriculationNumber.trim().toLowerCase();

        // establish LDAP connection and try to get user data
        LdapService ldap = LdapService.getInstance();
        try {
            ldap.connect(matriculationNumber, password);
        } catch (LdapService.LdapConnectorException e) {
            flash("error", e.getMessage());
            Component.addToContext(Component.ContextIdent.loginForm, form);
            return badRequest(landingpage.render());
        }

        // try to find user in DB, set role if found (default STUDENT role)
        Account account = Account.findByLoginName(matriculationNumber);
        AccountRole role = AccountRole.STUDENT;
        if (ldap.getRole() != null) {
            role = ldap.getRole();
        }

        // if user is not found in DB, create new user from LDAP data, otherwise update user data
        if (account == null) {
            account = new Account();
            Logger.info("New Account for " + matriculationNumber + " will be created.");
            account.firstname = ldap.getFirstName();
            account.lastname = ldap.getLastName();
            account.loginname = matriculationNumber;
            account.password = "LDAP - not needed";
            Random generator = new Random();
            account.avatar = String.valueOf(generator.nextInt(9));
            account.role = role;
            accountManager.create(account);
        } else {
            account.firstname = ldap.getFirstName();
            account.lastname = ldap.getLastName();
            account.role = role;
            accountManager.update(account);
        }

        // re-create session, set user
        session().clear();
        session("id", account.id.toString());
        if (rememberMe != null) {
            session("rememberMe", "1");
        }

        return redirect(redirect);
    }

    private Result defaultAuthenticate(final String redirect) {
        Form<Login> loginForm = form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            flash("error", loginForm.globalError().message());
            Component.addToContext(Component.ContextIdent.loginForm, loginForm);
            return badRequest(landingpage.render());
        } else {
            session().clear();
            session("email", loginForm.get().email);
            session("id", Account.findByEmail(loginForm.get().email).id.toString());
            session("firstname", Account.findByEmail(loginForm.get().email).firstname);
            if (loginForm.get().rememberMe != null) {
                session("rememberMe", "1");
            }

            return redirect(redirect);
        }
    }

    /**
     * Checks if the specified password is correct for the current used
     *
     * @param accountId the account which password should be checked
     * @param password  the password to check
     * @return true, if the password is correct
     */
    public static boolean checkPassword(Long accountId, String password) {
        Account account = Account.findById(accountId);

        if (password == null || password.length() == 0) {
            flash("error", Messages.get("Kein Passwort angegeben!"));
            return false;
        }

        if (account.loginname == null || account.loginname.length() == 0) { // not an LDAP Account
            Account auth = Account.authenticate(account.email, password);
            if (auth == null || auth.id != account.id) {
                flash("error", Messages.get("profile.delete.wrongpassword"));
                return false;
            } else {
                return true;
            }
        } else { // LDAP Account
            LdapService ldap = LdapService.getInstance();
            try {
                ldap.connect(account.loginname, password); // try logging in with the specified password
                return true; // login successful
            } catch (LdapService.LdapConnectorException e) {
                flash("error", e.getMessage());
                return false;
            }
        }
    }

    /**
     * Logout and clean the session.
     */
    public Result logout() {
        session().clear();
        flash("success", Messages.get("authenticate.logout"));

        return redirect(controllers.routes.Application.index());
    }
}
