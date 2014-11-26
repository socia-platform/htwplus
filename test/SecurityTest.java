import base.FakeApplicationTest;
import controllers.Secured;
import models.Account;
import models.Group;
import models.GroupAccount;
import models.enums.AccountRole;
import org.junit.Test;
import play.db.jpa.JPA;
import play.libs.F;

import static org.fest.assertions.Assertions.*;

/**
 * Testing security mechanisms.
 */
public class SecurityTest extends FakeApplicationTest {
    /**
     * Secured instance singleton instance.
     */
    private static Secured secured = null;

    /**
     * Returns the Secured singleton.
     *
     * @return Secured instance
     */
    protected static Secured getSecured() {
        if (SecurityTest.secured == null) {
            SecurityTest.secured = new Secured();
        }

        return SecurityTest.secured;
    }

    /**
     * Tests, if the method "getUsername()" works as expected.
     */
    @Test
    public void testGetUsername() {
        Account testAccount = this.getTestAccount(1);
        this.loginAccount(testAccount);
        assertThat(testAccount.id.toString()).isEqualTo(SecurityTest.getSecured().getUsername(this.getContext()));
    }

    /**
     * Tests, if the method "isAdmin()" works as expected.
     */
    @Test
    public void testIsAdmin() {
        // test, if test account is not admin
        this.loginTestAccount(1);
        assertThat(Secured.isAdmin()).isFalse();

        // test, if admin account is admin
        this.loginAdminAccount();
        assertThat(Secured.isAdmin()).isTrue();
    }

    /**
     * Tests, if the method "isMemberOfGroup()" works as expected.
     */
    @Test
    public void testIsMemberOfGroup() {
        final Account testAccount1 = this.getTestAccount(1);
        final Account testAccount2 = this.getTestAccount(2);
        final Account testAccount3 = this.getTestAccount(3);
        final Group testGroup = this.getTestGroup(1, testAccount1);
        this.establishGroupMembership(testAccount2, testGroup);
        this.removeGroupMembership(testAccount3, testGroup);

        // test, that we have exactly one notification
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                assertThat(Secured.isMemberOfGroup(testGroup, testAccount1)).isTrue();
                assertThat(Secured.isMemberOfGroup(testGroup, testAccount2)).isTrue();
                assertThat(Secured.isMemberOfGroup(testGroup, testAccount3)).isFalse();
            }
        });
    }

    /**
     * Tests, if the method "isOwnerOfAccount()" works as expected.
     */
    @Test
    public void testIsOwnerOfAccount() {
        final Account testAccount1 = this.getTestAccount(1);
        final Account testAccount2 = this.getTestAccount(2);
        this.loginAccount(testAccount1);

        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                assertThat(Secured.isOwnerOfAccount(testAccount1.id)).isTrue();
                assertThat(Secured.isOwnerOfAccount(testAccount2.id)).isFalse();
            }
        });
    }

    /**
     * Tests, if the method "isOwnerOfGroup()" works as expected.
     */
    @Test
    public void testIsOwnerOfGroup() {
        Account testAccount1 = this.getTestAccount(1);
        Account testAccount2 = this.getTestAccount(2);
        Group testGroup = this.getTestGroup(1, testAccount1);

        assertThat(Secured.isOwnerOfGroup(null, testAccount1)).isFalse();
        assertThat(Secured.isOwnerOfGroup(testGroup, testAccount1)).isTrue();
        assertThat(Secured.isOwnerOfGroup(testGroup, testAccount2)).isFalse();
    }

    /**
     * Tests, if the method "createCourse()" works as expected.
     */
    @Test
    public void testCreateCourse() {
        Account testAccount1 = this.getTestAccount(1);
        testAccount1.role = AccountRole.TUTOR;

        this.loginAccount(testAccount1);
        assertThat(Secured.createCourse()).isTrue();

        Account testAccount2 = this.getTestAccount(2);
        testAccount2.role = AccountRole.ADMIN;
        this.loginAccount(testAccount2);
        assertThat(Secured.createCourse()).isTrue();

        Account testAccount3 = this.getTestAccount(3);
        this.loginAccount(testAccount3);
        assertThat(Secured.createCourse()).isFalse();
    }

    /**
     * Tests, if the method "viewGroup()" works as expected.
     */
    @Test
    public void testViewGroup() {
        Account testAccount1 = this.getTestAccount(1);
        final Group testGroup = this.getTestGroup(1, testAccount1);

        // test, if admin is allowed to view
        this.loginAdminAccount();
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                assertThat(Secured.viewGroup(testGroup)).isTrue();
            }
        });

        // test, if member of group is allowed to view
        this.loginAccount(testAccount1);
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                assertThat(Secured.viewGroup(testGroup)).isTrue();
            }
        });

        // test, if no member of group is disallowed to view
        Account testAccount3 = this.getTestAccount(3);
        this.removeGroupMembership(testAccount3, testGroup);
        this.loginAccount(testAccount3);
        JPA.withTransaction(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                assertThat(Secured.viewGroup(testGroup)).isFalse();
            }
        });
    }

    /**
     * Tests, if the method "editGroup()" works as expected.
     */
    @Test
    public void testEditGroup() {
        Account testAccount1 = this.getTestAccount(1);
        final Group testGroup = this.getTestGroup(1, testAccount1);

        // test, if admin is allowed to edit
        this.loginAdminAccount();
        assertThat(Secured.editGroup(testGroup)).isTrue();

        // test, if owner of group is allowed to edit
        this.loginAccount(testAccount1);
        assertThat(Secured.editGroup(testGroup)).isTrue();

        // test, if not owner of group is disallowed to edit
        Account testAccount2 = this.getTestAccount(3);
        this.establishGroupMembership(testAccount2, testGroup);
        this.loginAccount(testAccount2);
        assertThat(Secured.editGroup(testGroup)).isFalse();
    }

    /**
     * Tests, if the method "deleteGroup()" works as expected.
     */
    @Test
    public void testDeleteGroup() {
        Account testAccount1 = this.getTestAccount(1);
        final Group testGroup = this.getTestGroup(1, testAccount1);

        // test, if admin is allowed to delete
        this.loginAdminAccount();
        assertThat(Secured.deleteGroup(testGroup)).isTrue();

        // test, if owner of group is allowed to delete
        this.loginAccount(testAccount1);
        assertThat(Secured.deleteGroup(testGroup)).isTrue();

        // test, if not owner of group is disallowed to delete
        Account testAccount2 = this.getTestAccount(3);
        this.establishGroupMembership(testAccount2, testGroup);
        this.loginAccount(testAccount2);
        assertThat(Secured.deleteGroup(testGroup)).isFalse();
    }

    /**
     * Tests, if the method "removeGroupMember()" works as expected.
     */
    @Test
    public void testRemoveGroupMember() {
        Account testAccount1 = this.getTestAccount(1);
        final Group testGroup = this.getTestGroup(1, testAccount1);

        // test, if no group returns false
        assertThat(Secured.removeGroupMember(null, testAccount1)).isFalse();

        // test, if owner of group cannot be removed
        this.loginAccount(testAccount1);
        assertThat(Secured.removeGroupMember(testGroup, testAccount1)).isFalse();

        // test, if admin is allowed to remove group member
        Account testAccount2 = this.getTestAccount(2);
        this.loginAdminAccount();
        assertThat(Secured.removeGroupMember(testGroup, testAccount2)).isTrue();

        // test, if no admin and no group owner can remove himself but no other from the group
        this.loginAccount(testAccount2);
        Account testAccount3 = this.getTestAccount(3);
        assertThat(Secured.removeGroupMember(testGroup, testAccount2)).isTrue();
        assertThat(Secured.removeGroupMember(testGroup, testAccount3)).isFalse();
    }

    /**
     * Tests, if the method "inviteMember()" works as expected.
     */
    @Test
    public void testInviteMember() {
        Account testAccount1 = this.getTestAccount(1);
        final Group testGroup = this.getTestGroup(1, testAccount1);

        // test, if no group returns false
        assertThat(Secured.inviteMember(null)).isFalse();

        // test, if owner of group can invite
        this.loginAccount(testAccount1);
        assertThat(Secured.inviteMember(testGroup)).isTrue();

        // test, if admin is allowed to invite
        this.loginAdminAccount();
        assertThat(Secured.inviteMember(testGroup)).isTrue();

        // test, if no owner is disallowed to invite
        Account testAccount2 = this.getTestAccount(2);
        this.loginAccount(testAccount2);
        assertThat(Secured.inviteMember(testGroup)).isFalse();
    }

    /**
     * Tests, if the method "acceptInvitation()" works as expected.
     */
    @Test
    public void testAcceptInvitation() {
        Account testAccount1 = this.getTestAccount(1);
        final Group testGroup = this.getTestGroup(1, testAccount1);

        // test, if no group returns false
        assertThat(Secured.acceptInvitation(null)).isFalse();

        // test, if the owner of the group can accept invitation
        this.loginAccount(testAccount1);
        GroupAccount groupAccount = this.getGroupAccount(testAccount1, testGroup);
        assertThat(Secured.acceptInvitation(groupAccount)).isTrue();

        // test, if the admin can accept invitation
        this.loginAdminAccount();
        assertThat(Secured.inviteMember(testGroup)).isTrue();
    }
}
