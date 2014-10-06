package base;

import controllers.Component;
import models.Account;
import models.Friendship;
import models.enums.AccountRole;
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
    private final Http.Request request = mock(Http.Request.class);
    public static FakeApplication app;

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
     * Returns a test account, creates one before if not exists.
     *
     * @param number Number of test account
     * @return Account instance
     */
    public Account getTestAccount(final int number) {
        try {
            return JPA.withTransaction(new F.Function0<Account>() {
                @Override
                public Account apply() throws Throwable {
                    String testAccountEmail = "test" + String.valueOf(number) + "@htwplus.de";
                    Account testAccount = Account.findByEmail(testAccountEmail);

                    // if there is this test account, return
                    if (testAccount != null) {
                        return testAccount;
                    }

                    // there is no test account with that number right now, create a persistent one
                    testAccount = new Account();
                    testAccount.firstname = "Test";
                    testAccount.lastname = "User " + String.valueOf(number);
                    testAccount.email = testAccountEmail;
                    testAccount.avatar = "a1";
                    testAccount.role = AccountRole.STUDENT;
                    testAccount.password = Component.md5("123456-654321");
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
     * Logs in a test account.
     *
     * @param testAccount Test account
     */
    public void loginTestAccount(Account testAccount) {
        Http.Context.current().session().put("id", testAccount.id.toString());
    }

    /**
     * Logs in a test account.
     *
     * @param testAccount Test account number
     */
    public void loginTestAccount(int testAccount) {
        Account a = this.getTestAccount(testAccount);

        this.loginTestAccount(a);
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
}
