package managers;

import models.Account;
import models.enums.AccountRole;
import models.services.LdapService;
import play.Logger;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.util.Random;

/**
 * Created by Iven on 21.08.2017.
 */
public class LoginManager {

    final Logger.ALogger LOG = Logger.of(LoginManager.class);

    AccountManager accountManager;
    LdapService ldapService;
    JPAApi jpaApi;

    @Inject
    public LoginManager(AccountManager accountManager, LdapService ldapService, JPAApi jpaApi) {
        this.accountManager = accountManager;
        this.ldapService = ldapService;
        this.jpaApi = jpaApi;
    }

    /**
     * Mail authentication.
     *
     * @return Result
     */
    public Account emailAuthenticate(final String email, final String password) {
        if (accountManager.isAccountValid(email, password)) {
            return accountManager.findByEmail(email);
        } else {
            return null;
        }
    }

    /**
     * LDAP authentication.
     *
     * @return Result
     */
    public Account ldapAuthenticate(final String loginName, final String loginPassword) throws LdapService.LdapConnectorException {

        // clean username
        String matriculationNumber = loginName.trim().toLowerCase();

        // try to connect
        ldapService.connect(matriculationNumber, loginPassword);

        // try to find user in DB, set role if found (default STUDENT role)
        Account account = accountManager.findByLoginName(matriculationNumber);
        AccountRole role = AccountRole.STUDENT;
        if (ldapService.getRole() != null) {
            role = ldapService.getRole();
        }

        // if user is not found in DB, create new user from LDAP data, otherwise update user data
        if (account == null) {
            LOG.info("LDAP entry not found. Creating new Account for: " + matriculationNumber);
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

        return account;
    }
}
