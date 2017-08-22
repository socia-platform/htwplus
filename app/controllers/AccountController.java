package controllers;

import managers.AccountManager;
import managers.LoginManager;
import models.Account;
import models.Login;
import models.services.LdapService;
import play.Logger;
import play.api.i18n.Lang;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.mvc.Result;
import views.html.landingpage;

import javax.inject.Inject;

/**
 * Controller for authenticate purposes.
 */
@Transactional
public class AccountController extends BaseController {

    final Logger.ALogger LOG = Logger.of(AccountController.class);

    AccountManager accountManager;
    FormFactory formFactory;
    LdapService ldapService;
    MessagesApi messagesApi;
    LoginManager loginManager;

    @Inject
    public AccountController(AccountManager accountManager, FormFactory formFactory, LdapService ldapService, MessagesApi messagesApi, LoginManager loginManager) {
        this.accountManager = accountManager;
        this.formFactory = formFactory;
        this.ldapService = ldapService;
        this.messagesApi = messagesApi;
        this.loginManager = loginManager;
    }

    /**
     * Default login action.
     *
     * @return Result
     */
    public Result login() {

        Form<Login> loginForm = formFactory.form(Login.class);
        Login login = loginForm.bindFromRequest().get();
        String loginName = login.loginName;
        String loginPassword = login.loginPassword;
        boolean rememberMe = login.rememberMe;
        Account accountToLogin = null;

        LOG.info("Login attempt from: " + loginName);

        // save originURL before clearing the session
        String redirect = session().get("originURL") == null ? "/" : session().get("originURL");
        session().clear();

        if (loginName.isEmpty()) {
            LOG.error("no name given");
            flash("error", "Also deine Matrikelnummer brauchen wir schon!");

            return badRequest(landingpage.render(loginName));

            // E-Mail authentication
        } else if (loginName.contains("@")) {
            accountToLogin = loginManager.emailAuthenticate(loginName, loginPassword);
            if (accountToLogin == null) {
                LOG.error("E-Mail not valid");
                flash("error", "Bitte melde dich mit deiner Matrikelnummer an.");
                Component.addToContext(Component.ContextIdent.loginForm, loginForm);

                return badRequest(landingpage.render(loginName));
            }

            // LDAP authentification
        } else if (loginName.length() > 0) {
            try {
                accountToLogin = loginManager.ldapAuthenticate(loginName, loginPassword);

            } catch (LdapService.LdapConnectorException e) {
                LOG.error("LDAP Error", e);
                flash("error", e.getMessage());
                Component.addToContext(Component.ContextIdent.loginForm, loginForm);
                return badRequest(landingpage.render(loginName));
            }

        }

        session("id", accountToLogin.id.toString());
        if (rememberMe) {
            session().put("rememberMe", "true");
        }

        LOG.info("Welcome, " + accountToLogin.name);
        LOG.info("Redirecting to " + redirect);

        return redirect(redirect);
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
        LOG.info("Bye, " + session().get("id"));
        session().clear();
        flash("success", messagesApi.get(Lang.defaultLang(), "authenticate.logout"));

        return redirect(controllers.routes.Application.index());
    }
}
