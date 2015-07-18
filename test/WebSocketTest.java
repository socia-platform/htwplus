import base.FakeApplicationTest;
import controllers.WebSocketController;
import mock.MockWebSocket;
import models.Account;
import models.services.JsonService;
import org.junit.*;

import static org.fest.assertions.Assertions.*;
import static org.junit.Assert.*;

/**
 * Testing WebSocket service.
 */
public class WebSocketTest extends FakeApplicationTest {
    /**
     * Tests, if a simple "Ping" is responded by "Pong".
     *
     * @throws Exception
     */
    @Test
    public void testPing() throws Exception {
        try {
            this.loginTestAccount(1);
            MockWebSocket ws = new MockWebSocket(WebSocketController.webSocket());
            ws.write(JsonService.getInstance().getJsonFromString("{\"method\": \"Ping\"}"));
            assertThat(ws.read().toString()).contains("Pong");
            ws.close();
            this.logoutTestAccount();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    /**
     * Tests, if a chat message if rejected, because sender is not logged in.
     *
     * @throws Exception
     */
    @Test
    public void testLoggedIn() throws Exception {
        // send WS message without user logged in
        try {
            Account testAccount = this.getTestAccount(1);
            MockWebSocket ws = new MockWebSocket(WebSocketController.webSocket());
            ws.write(JsonService.getInstance()
                    .getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": " + testAccount.id.toString() + "}")
            );
            assertThat(ws.read().toString()).contains("closed");
            assertThat(ws.read()).isNull();
            ws.close();
            this.logoutTestAccount();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    /**
     * Tests, if a chat message is rejected, because of recipient is not online.
     *
     * @throws Exception
     */
    @Test
    public void testOnline() throws Exception {
        try {
            this.loginTestAccount(1);
            Account testAccount2 = this.getTestAccount(2);

            MockWebSocket ws = new MockWebSocket(WebSocketController.webSocket());
            ws.write(JsonService.getInstance()
                            .getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": " + testAccount2.id.toString() + "}")
            );
            assertThat(ws.read().toString()).contains("Recipient not online");
            ws.close();
            this.logoutTestAccount();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    /**
     * Tests, if a chat message is rejected, because of sending to oneself.
     *
     * @throws Exception
     */
    @Test
    public void testChatYourself() throws Exception {
        try {
            Account testAccount = this.getTestAccount(1);
            this.loginAccount(testAccount);

            MockWebSocket ws = new MockWebSocket(WebSocketController.webSocket());
            ws.write(JsonService.getInstance()
                    .getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": " + testAccount.id.toString() + "}")
            );
            assertThat(ws.read().toString()).contains("Cannot send chat to yourself");
            ws.close();
            this.logoutTestAccount();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    /**
     * Tests, if a chat message is rejected, because of no established friendship.
     *
     * @throws Exception
     */
    @Test
    public void testFriendship() throws Exception {
        try {
            Account testAccountA = this.getTestAccount(3);
            Account testAccountB = this.getTestAccount(4);
            this.removeFriendshipTestAccounts(testAccountA, testAccountB);

            this.loginAccount(testAccountA);
            MockWebSocket ws1 = new MockWebSocket(WebSocketController.webSocket());

            this.loginAccount(testAccountB);
            MockWebSocket ws2 = new MockWebSocket(WebSocketController.webSocket());

            ws2.write(JsonService.getInstance()
                            .getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": " + testAccountA.id.toString() + "}")
            );

            assertThat(ws2.read().toString()).contains("You must be a friend of the recipient");

            ws1.close();
            ws2.close();
            this.logoutTestAccount();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    /**
     * Tests a successful sendable chat message.
     *
     * @throws Exception
     */
    @Test
    public void testChatSuccess() throws Exception {
        try {
            Account testAccountA = this.getTestAccount(1);
            Account testAccountB = this.getTestAccount(2);
            this.establishFriendshipTestAccounts(testAccountA, testAccountB);

            this.loginAccount(testAccountA);
            MockWebSocket ws1 = new MockWebSocket(WebSocketController.webSocket());

            this.loginAccount(testAccountB);
            MockWebSocket ws2 = new MockWebSocket(WebSocketController.webSocket());

            ws2.write(JsonService.getInstance()
                            .getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": " + testAccountA.id.toString() + "}")
            );
            assertThat(ws2.read().toString()).contains("OK");
            ws1.close();
            ws2.close();
            this.logoutTestAccount();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }
}