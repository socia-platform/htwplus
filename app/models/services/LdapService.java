package models.services;

import models.enums.AccountRole;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.exception.InvalidConnectionException;

import play.Logger;
import play.Play;
import play.i18n.Messages;

import java.io.IOException;

/**
 * LDAP service to establish LDAP connection and request user account data from directory.
 */
public class LdapService {
    /**
     * First name
     */
    private String firstName = null;

    /**
     * Last name
     */
    private String lastName = null;

    /**
     * Account role
     */
    private AccountRole role = null;

    /**
     * LDAP server host
     */
    String ldapServer = null;

    /**
     * LDAP port
     */
    int ldapPort;

    /**
     * If true, Start TLS is used for LDAP connection
     */
    boolean ldapStartTls;

    /**
     * Getter for first name.
     *
     * @return First name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Getter for last name.
     *
     * @return Last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Getter for account role.
     *
     * @return Account role
     */
    public AccountRole getRole() {
        return role;
    }

    /**
     * Singleton instance
     */
    private static LdapService instance = null;

    /**
     * Private constructor for singleton instance
     */
    private LdapService() {
        this.ldapServer = Play.application().configuration().getString("ldap.server");
        this.ldapPort = Integer.parseInt(Play.application().configuration().getString("ldap.port"));
        this.ldapStartTls = Boolean.parseBoolean(Play.application().configuration().getString("ldap.startTls"));
    }

    /**
     * Returns the singleton instance.
     *
     * @return NotificationHandler instance
     */
    public static LdapService getInstance() {
        if (LdapService.instance == null) {
            LdapService.instance = new LdapService();
        }

        return LdapService.instance;
    }

    /**
     * Connects to the LDAP ldapServer. If successful connected, the user is searched. If found, the user
     * data is read and set into firstName and lastName.
     *
     * @param userName User
     * @param password Password
     * @throws LdapConnectorException
     */
    public void connect(String userName, String password) throws LdapConnectorException {
        // connection and search related parameters
        String userRoot = Play.application().configuration().getString("ldap.userRoot");
        String groupRoot = Play.application().configuration().getString("ldap.groupRoot");
        String connectionBind = Play.application().configuration().getString("ldap.connectionBind")
                .replace("%USER%", userName)
                .replace("%USER_ROOT%", userRoot);
        String userSearch = Play.application().configuration().getString("ldap.userSearch")
                .replace("%USER%", userName);
        String groupSearch = Play.application().configuration().getString("ldap.groupSearch")
                .replace("%BIND%", connectionBind);

        // LDAP keys for values from LDAP ldapServer
        String userFirstName = Play.application().configuration().getString("ldap.serverValues.firstName");
        String userLastName = Play.application().configuration().getString("ldap.serverValues.lastName");
        String groupName = Play.application().configuration().getString("ldap.serverValues.groupName");
        String studentRole = Play.application().configuration().getString("ldap.serverValues.studentRole");
        String profRole = Play.application().configuration().getString("ldap.serverValues.profRole");
        String tutorRole = Play.application().configuration().getString("ldap.serverValues.tutorRole");

        LdapNetworkConnection ldapConnection;

        // try to connect to the LDAP ldapServer
        try {
            LdapConnectionConfig connectionConfig = new LdapConnectionConfig();
            connectionConfig.setLdapHost(this.ldapServer);
            connectionConfig.setLdapPort(this.ldapPort);
            connectionConfig.setUseTls(this.ldapStartTls);
            connectionConfig.setCredentials(password);
            connectionConfig.setName(connectionBind);
            ldapConnection = new LdapNetworkConnection(connectionConfig);
            ldapConnection.bind();
        } catch (InvalidConnectionException e) {
            e.printStackTrace();
            throw new LdapConnectorException(Messages.get("ldap.noConnection"));
        } catch (LdapException e) {
            e.printStackTrace();
            throw new LdapConnectorException(Messages.get("ldap.wrongCredentials"));
        }

        // login to LDAP successful, try to find the user data
        EntryCursor entCursor;
        try {
            entCursor = ldapConnection.search(userRoot, userSearch, SearchScope.ONELEVEL, "*");
            entCursor.next();
            Entry entry = entCursor.get();
            this.firstName = entry.get(userFirstName).getString();
            this.lastName = entry.get(userLastName).getString();
            Logger.info("Read Account from LDAP: " + this.firstName + " " + this.lastName);
        } catch (LdapException | CursorException e) {
            e.printStackTrace();
            throw new LdapConnectorException(Messages.get("ldap.wrongCredentials"));
        }

        // user data successfully set, try to find the role of the user
        try {
            entCursor =  ldapConnection.search(groupRoot, groupSearch, SearchScope.ONELEVEL, "*");
            String role;
            while (entCursor.next()) {
                Entry entry = entCursor.get();
                role = entry.get(groupName).getString();
                if (role.equals(studentRole)) {
                    this.role = AccountRole.STUDENT;
                }
                if (role.equals(profRole) || role.equals(tutorRole)) {
                    this.role = AccountRole.TUTOR;
                }
            }
        } catch (LdapException | CursorException e) {
            e.printStackTrace();
            throw new LdapConnectorException(Messages.get("ldap.wrongCredentials"));
        }

        // close the connection
        try {
            ldapConnection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Exception class for exceptions thrown in LdapConnector.
     */
    public class LdapConnectorException extends Exception{
        public LdapConnectorException(String message) {
            super(message);
        }

        private static final long serialVersionUID = 1L;
    }
}
