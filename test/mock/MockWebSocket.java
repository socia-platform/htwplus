package mock;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.WebSocket;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.TimeUnit;

/**
 * Credits: http://vlkan.com/blog/post/2014/08/07/testing-websockets-in-play/
 */
@ThreadSafe
public class MockWebSocket {
    protected final MockInputWebSocket mockInput = new MockInputWebSocket();
    protected final MockOutputWebSocket mockOutput = new MockOutputWebSocket();
    protected final WebSocket<JsonNode> socket;

    public MockWebSocket(WebSocket<JsonNode> socket) {
        this.socket = socket;
        socket.onReady(mockInput.getInputSocket(), mockOutput.getOutputSocket());
    }

    public JsonNode read() throws InterruptedException {
        return mockOutput.getMessageQueue().poll(1, TimeUnit.SECONDS);
    }

    public void write(JsonNode data) throws Throwable { mockInput.write(data); }

    public void close() throws Throwable { mockInput.close(); }
}
