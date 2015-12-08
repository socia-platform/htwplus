package services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.Component;
import models.Account;
import models.Group;
import models.enums.AccountRole;
import models.enums.GroupType;
import models.services.EmailService;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Iven on 02.12.2015.
 */
@Singleton
public class PostgresInit implements DatabaseService {

    private JPAApi jpaApi;
    private Account adminAccount;
    private Account dummyAccount;
    private Group adminGroup;
    private Group feedbackGroup;

    private final Config conf = ConfigFactory.load();
    private final String adminGroupTitle = conf.getString("htwplus.admin.group");
    private final String adminMail = conf.getString("htwplus.admin.mail");
    private final String adminPassword = conf.getString("htwplus.admin.pw");
    private final String dummyMail = conf.getString("htwplus.dummy.mail");
    private final String dummyPassword = conf.getString("htwplus.dummy.pw");

    @Inject
    public PostgresInit(JPAApi jpaApi, Account adminAccount, Account dummyAccount, Group adminGroup, Group feedbackGroup) {
        this.jpaApi = jpaApi;
        this.adminAccount = adminAccount;
        this.dummyAccount = dummyAccount;
        this.adminGroup = adminGroup;
        this.feedbackGroup = feedbackGroup;
        initialization();
    }

    @Override
    public void initialization() {
        jpaApi.withTransaction(() -> {
            if (adminAccount.findByEmail(adminMail) == null) {
                adminAccount.email = adminMail;
                adminAccount.firstname = "Admin";
                adminAccount.lastname = "@HTWplus";
                adminAccount.role = AccountRole.ADMIN;
                adminAccount.avatar = "a1";
                adminAccount.password = Component.md5(adminPassword);
                adminAccount.create();
            }

            // create Dummy anonymous account, if it doesn't exist //
            if (dummyAccount.findByEmail(dummyMail) == null) {
                dummyAccount.email = dummyMail;
                dummyAccount.firstname = "Gelöschter";
                dummyAccount.lastname = "Account";
                dummyAccount.role = AccountRole.DUMMY;
                dummyAccount.avatar = "aDefault";
                dummyAccount.password = Component.md5(dummyPassword);
                dummyAccount.create();
            }

            // create Admin group if none exists
            if (Group.findByTitle(adminGroupTitle) == null) {
                adminGroup.title = adminGroupTitle;
                adminGroup.groupType = GroupType.close;
                adminGroup.description = "for HTWplus Admins only";
                adminGroup.createWithGroupAccount(adminAccount);
            }

            // create Feedback group if none exists
            if (Group.findByTitle("HTWplus Feedback") == null) {
                feedbackGroup.title = "HTWplus Feedback";
                feedbackGroup.groupType = GroupType.open;
                feedbackGroup.description = "Du hast Wünsche, Ideen, Anregungen, Kritik oder Probleme mit der Seite? Hier kannst du es loswerden!";
                feedbackGroup.createWithGroupAccount(adminAccount);
            }
        });




    }
}
