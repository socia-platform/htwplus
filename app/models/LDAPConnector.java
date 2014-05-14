package models;

import models.enums.AccountRole;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.exception.InvalidConnectionException;

import play.Logger;

public class LDAPConnector {
	
	private String server = "portia.f4.htw-berlin.de";
	private int port = 389;
	private String root = "ou=users,o=f4,dc=htw-berlin,dc=de";
	private String groupRoot = "ou=groups,o=f4,dc=htw-berlin,dc=de";
	private String studentIdent = "student";
	private String tutorIdent = "lehrende";
	
	private String username = null;
	private String password = null;
	private String firstname = null;
	private String lastname = null;

	private AccountRole role = null;
	
	LdapConnection conn = null;
		
	public String getUsername() {
		return username;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}
	
	public AccountRole getRole() {
		return role;
	}
	
	public LDAPConnector() {
		conn = new LdapNetworkConnection(server, port);
	}
	
	public void connect(String username, String password) throws LDAPConnectorException {
		this.username = username;
		this.password = password;
		
		try {
			conn.bind("uid=" + this.username + "," + this.root, this.password);
		} catch (InvalidConnectionException e) {
			e.printStackTrace();
			throw new LDAPConnectorException("Keine Verbindung zum LDAP-Server möglich. Bitte versuche es später noch einmal.");
		} catch (LdapException e) {
			e.printStackTrace();
			throw new LDAPConnectorException("Ungültiges Passwort oder Nutzername.");
		}
	
		EntryCursor entCursor = null;
    	try {
			entCursor =  conn.search(this.root, "(uid=" + this.username + ")", SearchScope.ONELEVEL, "*");
			entCursor.next();
			Entry entry = entCursor.get();
			this.firstname = entry.get("givenName").getString();
			this.lastname = entry.get("sn").getString();
			Logger.info("Read Account from LDAP: " + this.firstname + " " + this.lastname);
		} catch (LdapException | CursorException e) {
			e.printStackTrace();
			throw new LDAPConnectorException("Es gab ein Problem bei der Verbindung zum LDAP-Server. Bitte versuche es später noch einmal.");
		}
    	
    	try {
			entCursor =  conn.search(this.groupRoot, "(memberUid=" + this.username + ")", SearchScope.ONELEVEL, "*");
			String role = null;
	    	while(entCursor.next()) {
	    		Entry entry = entCursor.get();
	    		role = entry.get("cn").getString();
	    		if(role.equals(this.studentIdent)) {
	    			this.role = AccountRole.STUDENT;
	    		}
	    		if(role.equals(this.tutorIdent)){
	    			this.role = AccountRole.TUTOR;
	    		}
	    	}
		} catch (LdapException | CursorException e) {
			e.printStackTrace();
			throw new LDAPConnectorException("Es gab ein Problem bei der Verbindung zum LDAP-Server. Bitte versuche es später noch einmal.");
		}
    	
	}
		
	public class LDAPConnectorException extends Exception{
		public LDAPConnectorException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;
	}
}
