package services;

import controllers.Component;
import managers.AccountManager;
import managers.GroupManager;
import models.Account;
import models.Group;
import models.enums.AccountRole;
import models.enums.GroupType;
import play.Configuration;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Iven on 02.12.2015.
 */
@Singleton
public class PostgresInit implements DatabaseService {

    JPAApi jpaApi;
    AccountManager accountManager;
    GroupManager groupManager;
    Configuration configuration;

    @Inject
    public PostgresInit(JPAApi jpaApi, AccountManager accountManager, GroupManager groupManager, Configuration configuration) {
        this.jpaApi = jpaApi;
        this.accountManager = accountManager;
        this.groupManager = groupManager;
        this.configuration = configuration;
        initialization();
    }

    @Override
    public void initialization() {

        final String adminGroupTitle = configuration.getString("htwplus.admin.group");
        final String adminMail = configuration.getString("htwplus.admin.mail");
        final String adminPassword = configuration.getString("htwplus.admin.pw");
        final String dummyMail = configuration.getString("htwplus.dummy.mail");
        final String dummyPassword = configuration.getString("htwplus.dummy.pw");


        this.jpaApi.withTransaction(() -> {

            // create admin account if none exists
            Account adminAccount = accountManager.findByEmail(adminMail);
            if (adminAccount == null) {
                adminAccount = new Account();
                adminAccount.email = adminMail;
                adminAccount.firstname = "Admin";
                adminAccount.lastname = "@HTWplus";
                adminAccount.role = AccountRole.ADMIN;
                adminAccount.avatar = "1";
                adminAccount.password = Component.md5(adminPassword);
                accountManager.create(adminAccount);
            }

            // create dummy account if none exists
            if (accountManager.findByEmail(dummyMail) == null) {
                Account dummyAccount = new Account();
                dummyAccount.email = dummyMail;
                dummyAccount.firstname = "Gelöschter";
                dummyAccount.lastname = "Account";
                dummyAccount.role = AccountRole.DUMMY;
                dummyAccount.avatar = "1";
                dummyAccount.password = Component.md5(dummyPassword);
                accountManager.create(dummyAccount);
            }

            // create admin group if none exists
            if (groupManager.findByTitle(adminGroupTitle) == null) {
                Group adminGroup = new Group();
                adminGroup.title = adminGroupTitle;
                adminGroup.groupType = GroupType.close;
                adminGroup.description = "for HTWplus Admins only";
                groupManager.createWithGroupAccount(adminGroup, adminAccount);
            }

            // create feedback group if none exists
            if (groupManager.findByTitle("HTWplus Feedback") == null) {
                Group feedbackGroup = new Group();
                feedbackGroup.title = "HTWplus Feedback";
                feedbackGroup.groupType = GroupType.open;
                feedbackGroup.description = "Du hast Wünsche, Ideen, Anregungen, Kritik oder Probleme mit der Seite? Hier kannst du es loswerden!";
                groupManager.createWithGroupAccount(feedbackGroup, adminAccount);
            }
        });

    }
}
