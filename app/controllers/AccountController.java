package controllers;

import managers.AccountManager;
import models.Account;
import models.Login;
import models.enums.AccountRole;
import models.services.LdapService;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.api.i18n.Lang;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.mvc.Result;
import views.html.landingpage;

import javax.inject.Inject;
import java.util.Random;

/**
 * Controller for authenticate purposes.
 */
@Transactional
public class AccountController extends BaseController {

    final Logger.ALogger LOG = Logger.of(AccountController.class);

    private final AccountManager accountManager;
    private final FormFactory formFactory;
    private final LdapService ldapService;
    private final MessagesApi messagesApi;

    @Inject
    public AccountController(AccountManager accountManager, FormFactory formFactory, LdapService ldapService, MessagesApi messagesApi) {
        this.accountManager = accountManager;
        this.formFactory = formFactory;
        this.ldapService = ldapService;
        this.messagesApi = messagesApi;
    }

    /**
     * Default authentication action.
     *
     * @return Result
     */
    public Result authenticate() {
        Form<Login> form = formFactory.form(Login.class).bindFromRequest();
        String username = form.field("email").value();

        // save originURL before clearing the session (it gets cleared in defaultAuthenticate() and LdapAuthenticate())
        String redirect = session().get("originURL");
        if (redirect == null) redirect = "/";

        LOG.info("Login attempt from: " + username);
        LOG.info("Redirecting to " + redirect);
        if (username.contains("@")) {
            return emailAuthenticate(redirect);
        } else if (username.length() == 0) {
            LOG.info("... no name given");
            flash("error", "Also deine Matrikelnummer brauchen wir schon!");
            return badRequest(landingpage.render(form));
        } else {
            return ldapAuthenticate(redirect);
        }
    }

    /**
     * LDAP authentication.
     *
     * @return Result
     */
    private Result ldapAuthenticate(final String redirect) {
        Form<Login> form = formFactory.form(Login.class).bindFromRequest();
        String matriculationNumber = form.field("email").value();
        String password = form.field("password").value();
        String rememberMe = form.field("rememberMe").value();

        // Clean the username
        matriculationNumber = matriculationNumber.trim().toLowerCase();

        try {
            ldapService.connect(matriculationNumber, password);
        } catch (LdapService.LdapConnectorException e) {
            flash("error", e.getMessage());
            Component.addToContext(Component.ContextIdent.loginForm, form);
            return badRequest(landingpage.render(form));
        }

        // try to find user in DB, set role if found (default STUDENT role)
        Account account = accountManager.findByLoginName(matriculationNumber);
        AccountRole role = AccountRole.STUDENT;
        if (ldapService.getRole() != null) {
            role = ldapService.getRole();
        }

        // if user is not found in DB, create new user from LDAP data, otherwise update user data
        if (account == null) {
            LOG.info("... not found. Creating new Account for: " + matriculationNumber);
            account = new Account();
            account.firstname = ldapService.getFirstName();
            account.lastname = ldapService.getLastName();
            account.loginname = matriculationNumber;
            account.password = "LDAP - not needed";
            Random generator = new Random();
            account.avatar = String.valueOf(generator.nextInt(9));
            account.role = role;
            accountManager.create(account);
        } else {
            account.firstname = ldapService.getFirstName();
            account.lastname = ldapService.getLastName();
            account.role = role;
            accountManager.update(account);
        }

        // re-create session, set user
        session().clear();
        session("id", account.id.toString());
        if (rememberMe != null) {
            session("rememberMe", "1");
        }
        LOG.info("Welcome, " + account.name);

        return redirect(redirect);
    }

    /**
     * Mail authentication.
     *
     * @return Result
     */
    private Result emailAuthenticate(final String redirect) {
        Form<Login> loginForm = formFactory.form(Login.class).bindFromRequest();
        Login login = loginForm.get();
        if (!accountManager.isAccountValid(login.email, login.password)) {
            LOG.info("User/Password not valid");
            flash("error", "Bitte melde dich mit deiner Matrikelnummer an.");
            Component.addToContext(Component.ContextIdent.loginForm, loginForm);
            return badRequest(landingpage.render(loginForm));
        } else {
            Account account = accountManager.findByEmail(login.email);
            session().clear();
            session("email", loginForm.get().email);
            session("id", account.id.toString());
            session("firstname", account.firstname);
            if (loginForm.get().rememberMe != null) {
                session("rememberMe", "1");
            }
            LOG.info("Welcome, " + account.name);
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
    public boolean checkPassword(Long accountId, String password) {
        Account account = accountManager.findById(accountId);

        if (password == null || password.length() == 0) {
            flash("error", messagesApi.get(Lang.defaultLang(), "Kein Passwort angegeben!"));
            return false;
        }

        if (account.loginname == null || account.loginname.length() == 0) { // not an LDAP Account
            if (accountManager.isAccountValid(account.email, password)) {
                flash("error", messagesApi.get(Lang.defaultLang(), "profile.delete.wrongpassword"));
                return false;
            } else {
                return true;
            }
        } else { // LDAP Account
            try {
                ldapService.connect(account.loginname, password); // try logging in with the specified password
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
        LOG.info("Bye, " + session().get("email"));
        session().clear();
        flash("success", messagesApi.get(Lang.defaultLang(), "authenticate.logout"));

        return redirect(controllers.routes.Application.index());
    }
}
