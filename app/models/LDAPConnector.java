package models;

import models.enums.AccountRole;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.exception.InvalidConnectionException;

import play.Logger;
import play.Play;
import play.i18n.Messages;

/**
 * LDAP Connector to establish LDAP connection and request user account data from directory.
 */
public class LDAPConnector {
    /**
     * First name
     */
    private String firstName = null;

    /**
     * Last name
     */
    private String lastName = null;

    /**
     * E-Mail
     */
    private String email = null;

    /**
     * Account role
     */
    private AccountRole role = null;

    /**
     * LDAP connection reference
     */
    LdapConnection connection = null;

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
     * Getter for email.
     *
     * @return E-Mail
     */
    public String getEmail() {
        return this.email;
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
     * Constructor.
     */
    public LDAPConnector() {
        String server = Play.application().configuration().getString("ldap.server");
        int port = Integer.parseInt(Play.application().configuration().getString("ldap.port"));
        boolean startTls = Boolean.parseBoolean(Play.application().configuration().getString("ldap.startTls"));

        LdapConnectionConfig connectionConfig = new LdapConnectionConfig();
        connectionConfig.setLdapHost(server);
        connectionConfig.setLdapPort(port);
        connectionConfig.setUseTls(startTls);

        this.connection = new LdapNetworkConnection(connectionConfig);
    }

    /**
     * Connects to the LDAP server. If successful connected, the user is searched. If found, the user
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

        // LDAP keys for values from LDAP server
        String userFirstName = Play.application().configuration().getString("ldap.serverValues.firstName");
        String userLastName = Play.application().configuration().getString("ldap.serverValues.lastName");
        String userEmail = Play.application().configuration().getString("ldap.serverValues.email");
        String groupName = Play.application().configuration().getString("ldap.serverValues.groupName");
        String studentRole = Play.application().configuration().getString("ldap.serverValues.studentRole");
        String tutorRole = Play.application().configuration().getString("ldap.serverValues.tutorRole");

        // try to connect to the LDAP server
        try {
            this.connection.bind(connectionBind, password);
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
            entCursor = this.connection.search(userRoot, userSearch, SearchScope.ONELEVEL, "*");
            entCursor.next();
            Entry entry = entCursor.get();
            this.firstName = entry.get(userFirstName).getString();
            this.lastName = entry.get(userLastName).getString();
            this.email = entry.get(userEmail).getString();
            Logger.info("Read Account from LDAP: " + this.firstName + " " + this.lastName + " (E-Mail: " + this.email + ")");
        } catch (LdapException | CursorException e) {
            e.printStackTrace();
            throw new LdapConnectorException(Messages.get("ldap.generalError"));
        }

        // user data successfully set, try to find the role of the user
        try {
            entCursor =  connection.search(groupRoot, groupSearch, SearchScope.ONELEVEL, "*");
            String role;
            while (entCursor.next()) {
                Entry entry = entCursor.get();
                role = entry.get(groupName).getString();
                if (role.equals(studentRole)) {
                    this.role = AccountRole.STUDENT;
                }
                if (role.equals(tutorRole)) {
                    this.role = AccountRole.TUTOR;
                }
            }
        } catch (LdapException | CursorException e) {
            e.printStackTrace();
            throw new LdapConnectorException(Messages.get("ldap.generalError"));
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
