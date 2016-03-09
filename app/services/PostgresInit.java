package services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.Component;
import managers.AccountManager;
import managers.GroupManager;
import models.Account;
import models.Group;
import models.enums.AccountRole;
import models.enums.GroupType;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Iven on 02.12.2015.
 */
@Singleton
public class PostgresInit implements DatabaseService {

    private JPAApi jpaApi;
    AccountManager accountManager;
    GroupManager groupManager;

    private final Config conf = ConfigFactory.load();
    private final String adminGroupTitle = conf.getString("htwplus.admin.group");
    private final String adminMail = conf.getString("htwplus.admin.mail");
    private final String adminPassword = conf.getString("htwplus.admin.pw");
    private final String dummyMail = conf.getString("htwplus.dummy.mail");
    private final String dummyPassword = conf.getString("htwplus.dummy.pw");

    @Inject
    public PostgresInit(JPAApi jpaApi, AccountManager accountManager, GroupManager groupManager) {
        this.jpaApi = jpaApi;
        this.accountManager = accountManager;
        this.groupManager = groupManager;
        initialization();
    }

    @Override
    public void initialization() {
        jpaApi.withTransaction(() -> {

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
