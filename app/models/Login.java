package models;

import play.db.jpa.Transactional;

public class Login {

		public String email;
		public String password;
		public String rememberMe;

		@Transactional
		public String validate() {
			if (Account.authenticate(this.email, this.password) == null) {
				return "Bitte melde dich mit deiner Matrikelnummer an.";
			}
			return null;
	}
}