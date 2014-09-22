import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.WebSocketController;
import mock.MockWebSocket;
import org.junit.*;
import play.mvc.Http;
import play.test.FakeApplication;
import play.test.Helpers;

import java.util.Collections;
import java.util.Map;

import static org.fest.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Testing WebSocket service
 */
public class WebSocketTest {
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
        Http.Context context = new Http.Context(id, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);
        Http.Context.current().session().put("id", null);
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    @Test
    public void testLoggedIn() throws Exception {
        // send WS message without user logged in
        try {
            MockWebSocket ws = new MockWebSocket(WebSocketController.webSocket());
            ws.write(this.getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": 7}"));
            assertThat(ws.read().toString()).contains("closed");
            assertThat(ws.read()).isNull();
            ws.close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    @Test
    public void testOnline() throws Exception {
        try {
            Http.Context.current().session().replace("id", "1");
            MockWebSocket ws = new MockWebSocket(WebSocketController.webSocket());
            ws.write(this.getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": 7}"));
            assertThat(ws.read().toString()).contains("Recipient not online");
            ws.close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    @Test
    public void testChatYourself() throws Exception {
        try {
            Http.Context.current().session().replace("id", "1");
            MockWebSocket ws = new MockWebSocket(WebSocketController.webSocket());
            ws.write(this.getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": 1}"));
            assertThat(ws.read().toString()).contains("Cannot send chat to yourself");
            ws.close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    @Test
    public void testChatSuccess() throws Exception {
        try {
            Http.Context.current().session().replace("id", "7");
            MockWebSocket ws1 = new MockWebSocket(WebSocketController.webSocket());
            Http.Context.current().session().replace("id", "181");
            MockWebSocket ws2 = new MockWebSocket(WebSocketController.webSocket());
            ws2.write(this.getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": 7}"));
            assertThat(ws2.read().toString()).contains("OK");
            ws1.close();
            ws2.close();
            System.out.println(ws1.read().toString());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    @Test
    public void testFriendship() throws Exception {
        try {
            // fake another login (user 7) and try to send to user 1 (they are no friends)
            Http.Context.current().session().replace("id", "1");
            MockWebSocket ws1 = new MockWebSocket(WebSocketController.webSocket());
            Http.Context.current().session().replace("id", "7");
            MockWebSocket ws2 = new MockWebSocket(WebSocketController.webSocket());
            ws2.write(this.getJsonFromString("{\"method\": \"SendChat\", \"text\": \"Huhu\", \"recipient\": 1}"));
            assertThat(ws2.read().toString()).contains("You must be a friend of the recipient");
            ws1.close();
            ws2.close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail();
        }
    }

    private JsonNode getJsonFromString(String jsonString) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(jsonString);
    }
}