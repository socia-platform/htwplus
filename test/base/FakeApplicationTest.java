package base;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.Component;
import models.Account;
import models.Friendship;
import models.Group;
import models.GroupAccount;
import models.enums.AccountRole;
import models.enums.GroupType;
import models.enums.LinkType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.db.jpa.JPA;
import play.libs.F;
import play.mvc.Http;
import play.test.FakeApplication;
import play.test.Helpers;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * You can extend your test classes from this class to provide a complete fake environment.
 */
public abstract class FakeApplicationTest {
    /**
     * Mocks a HTTP request instance.
     */
    private final Http.Request request = mock(Http.Request.class);

    /**
     * Hols the FakeApplication instance.
     */
    public static FakeApplication app;

    /**
     * The default password for test accounts.
     */
    public static final String TEST_ACCOUNT_PASSWORD = "123456";

    /**
     * Holds the singleton Config instance for the current context.
     */
    private static Config configuration = null;

    /**
     * Returns the configuration value for a configuration key.
     *
     * @param configurationKey Configuration key
     * @return Configuration value
     */
    protected static String getConfiguration(String configurationKey) {
        if (FakeApplicationTest.configuration == null) {
            FakeApplicationTest.configuration = ConfigFactory.load();
        }

        return FakeApplicationTest.configuration.getString(configurationKey);
    }

    @BeforeClass
    public static void startApp() {
        app = Helpers.fakeApplication();
        Helpers.start(app);
    }

    @Before
    public void setUp() throws Exception {
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        Long id = 2L;
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Context context = new Http.Context(id, header, this.request, flashData, flashData, argData);
        Http.Context.current.set(context);
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    /**
     * Returns an account by email.
     *
     * @param email E-Mail address of account to fetch
     * @return Account instance
     */
    public Account getAccountByEmail(final String email) {
        try {
            return JPA.withTransaction(new F.Function0<Account>() {
                @Override
                public Account apply() throws Throwable {
                    return Account.findByEmail(email);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a test account, creates one before if not exists.
     *
     * @param number Number of test account
     * @return Account instance
     */
    public Account getTestAccount(final int number) {
        final String testAccountEmail = "test" + String.valueOf(number) + "@htwplus.de";
        Account storedTestAccount = this.getAccountByEmail(testAccountEmail);

        // if there is this test account, return
        if (storedTestAccount != null) {
            return storedTestAccount;
        }

        // there is no test account with that number right now, create a persistent one
        try {
            return JPA.withTransaction(new F.Function0<Account>() {
                @Override
                public Account apply() throws Throwable {
                    Account testAccount = new Account();
                    testAccount.firstname = "Test";
                    testAccount.lastname = "User " + String.valueOf(number);
                    testAccount.email = testAccountEmail;
                    testAccount.avatar = "a1";
                    testAccount.role = AccountRole.STUDENT;
                    testAccount.password = Component.md5(FakeApplicationTest.TEST_ACCOUNT_PASSWORD);
                    testAccount.create();

                    return testAccount;
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the default admin account.
     *
     * @return Account instance
     */
    public Account getAdminAccount() {
        String adminEmailAddress = FakeApplicationTest.getConfiguration("htwplus.admin.mail");
        return this.getAccountByEmail(adminEmailAddress);
    }

    /**
     * Establishes a friendship between test accounts.
     *
     * @param testAccountA First test account
     * @param testAccountB Second test account
     */
    public void establishFriendshipTestAccounts(final Account testAccountA, final Account testAccountB) {
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                if (!Friendship.alreadyFriendly(testAccountA, testAccountB)) {
                    Friendship testFriendship = new Friendship(testAccountA, testAccountB, LinkType.establish);
                    testFriendship.create();
                }
                if (!Friendship.alreadyFriendly(testAccountB, testAccountA)) {
                    Friendship testFriendship = new Friendship(testAccountB, testAccountA, LinkType.establish);
                    testFriendship.create();
                }
            }
        });
    }

    /**
     * Removes a friendship between test accounts.
     *
     * @param testAccountA First test account
     * @param testAccountB Second test account
     */
    public void removeFriendshipTestAccounts(final Account testAccountA, final Account testAccountB) {
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                if (Friendship.alreadyFriendly(testAccountA, testAccountB)) {
                    Friendship.findFriendLink(testAccountA, testAccountB).delete();
                }
                if (Friendship.alreadyFriendly(testAccountB, testAccountA)) {
                    Friendship.findFriendLink(testAccountB, testAccountA).delete();
                }
            }
        });
    }


    /**
     * Returns a group by title.
     *
     * @param title Title of group to fetch
     * @return Group instance
     */
    public Group getGroupByTitle(final String title) {
        try {
            return JPA.withTransaction(new F.Function0<Group>() {
                @Override
                public Group apply() throws Throwable {
                    return Group.findByTitle(title);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a test group, creates one before if not exists.
     *
     * @param number Number of test group
     * @param groupOwner the optional group owner
     * @return Group instance
     */
    public Group getTestGroup(final int number, final Account groupOwner) {
        final String testGroupTitle = "Test Group " + String.valueOf(number);
        Group storedTestGroup = this.getGroupByTitle(testGroupTitle);

        // if there is this test group, return
        if (storedTestGroup != null) {
            return storedTestGroup;
        }

        // there is no test account with that number right now, create a persistent one
        try {
            return JPA.withTransaction(new F.Function0<Group>() {
                @Override
                public Group apply() throws Throwable {
                    Group testGroup = new Group();
                    testGroup.groupType = GroupType.close;
                    testGroup.setTitle(testGroupTitle);

                    if (groupOwner != null) {
                        testGroup.createWithGroupAccount(groupOwner);
                    } else {
                        testGroup.create();
                    }

                    return testGroup;
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a group account for an account to a group.
     *
     * @param account Account
     * @param group Group
     * @return GroupAccount instance if found, otherwise null
     */
    public GroupAccount getGroupAccount(final Account account, final Group group) {
        try {
            return JPA.withTransaction(new F.Function0<GroupAccount>() {
                @Override
                public GroupAccount apply() throws Throwable {
                    return GroupAccount.find(account, group);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    /**
     * Establishes a group membership of an account to a group.
     *
     * @param account Account
     * @param group Group
     */
    public void establishGroupMembership(final Account account, final Group group) {
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                if (!Group.isMember(group, account)) {
                    GroupAccount testGroupAccount = new GroupAccount(account, group, LinkType.establish);
                    testGroupAccount.create();
                    group.update();
                }
            }
        });
    }

    /**
     * Removes a membership of an account to a group.
     *
     * @param account Account
     * @param group Group
     */
    public void removeGroupMembership(final Account account, final Group group) {
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                if (Group.isMember(group, account)) {
                    GroupAccount groupAccount = GroupAccount.find(account, group);
                    groupAccount.delete();
                }
            }
        });
    }

    /**
     * Logs in an account into the test HTTP context.
     *
     * @param account Account to login
     */
    public void loginAccount(Account account) {
        Http.Context.current().session().put("id", account.id.toString());
        Http.Context.current().args.put("account", account);
    }

    /**
     * Logs in a test account.
     *
     * @param testAccountNumber Test account number
     */
    public void loginTestAccount(int testAccountNumber) {
        Account testAccount = this.getTestAccount(testAccountNumber);
        this.loginAccount(testAccount);
    }

    /**
     * Logs in the default admin account.
     */
    public void loginAdminAccount() {
        Account adminAccount = this.getAdminAccount();
        this.loginAccount(adminAccount);
    }

    /**
     * Logs out a test account.
     */
    public void logoutTestAccount() {
        Http.Context.current().session().remove("id");
    }

    /**
     * Force a thread sleep.
     *
     * @param seconds Seconds how long to sleep
     */
    protected void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the current HTTP context.
     *
     * @return Context instance
     */
    protected Http.Context getContext() {
        return Http.Context.current();
    }
}
