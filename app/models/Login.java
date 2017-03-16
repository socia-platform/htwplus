package models;

import managers.AccountManager;
import play.db.jpa.Transactional;

import javax.inject.Inject;

public class Login {

    public String email;
    public String password;
    public String rememberMe;

    @Inject
    AccountManager accountManager;

    @Transactional
    public String validate() {
        if (accountManager.authenticate(this.email, this.password) == null) {
            return "Bitte melde dich mit deiner Matrikelnummer an.";
        }
        return null;
    }
}