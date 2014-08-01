package models;

import play.db.jpa.Transactional;

public class Login {

		public String email;
		public String password;
		public String rememberMe;

		@Transactional
		public String validate() {
			if (Account.authenticate(email, password) == null) {
				return "Ungültiges Passwort oder Email-Adresse.";
			}
			return null;
		}
	}