package services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.Component;
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
    private Account adminAccount;
    private Group adminGroup;
    private Group feedbackGroup;

    private final Config conf = ConfigFactory.load();
    private final String adminGroupTitle = conf.getString("htwplus.admin.group");
    private final String adminMail = conf.getString("htwplus.admin.mail");
    private final String adminPassword = conf.getString("htwplus.admin.pw");

    @Inject
    public PostgresInit(JPAApi jpaApi, Account adminAccount, Group adminGroup, Group feedbackGroup) {
        this.jpaApi = jpaApi;
        this.adminAccount = adminAccount;
        this.adminGroup = adminGroup;
        this.feedbackGroup = feedbackGroup;
        initialization();
    }

    @Override
    public void initialization() {
        jpaApi.withTransaction(() -> {
            if (Account.findByEmail(adminMail) == null) {
                adminAccount.email = adminMail;
                adminAccount.firstname = "Admin";
                adminAccount.lastname = "@HTWplus";
                adminAccount.role = AccountRole.ADMIN;
                adminAccount.avatar = "a1";
                adminAccount.password = Component.md5(adminPassword);
                adminAccount.create();
            }
        });

        /**
         // create Admin group if none exists
         if (Group.findByTitle(adminGroupTitle) == null) {
         adminGroup.title = adminGroupTitle;
         adminGroup.groupType = GroupType.close;
         adminGroup.description = "for HTWplus Admins only";
         adminGroup.createWithGroupAccount(adminAccount);
         }

         // create Feedback group if none exists
         if (Group.findByTitle("HTWplus Feedback") == null) {
         feedbackGroup = new Group();
         feedbackGroup.title = "HTWplus Feedback";
         feedbackGroup.groupType = GroupType.open;
         feedbackGroup.description = "Du hast Wünsche, Ideen, Anregungen, Kritik oder Probleme mit der Seite? Hier kannst du es loswerden!";
         feedbackGroup.createWithGroupAccount(adminAccount);
         }
         */
    }
}
