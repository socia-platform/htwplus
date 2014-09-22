package mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.mvc.WebSocket;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Credits: http://vlkan.com/blog/post/2014/08/07/testing-websockets-in-play/
 */
@ThreadSafe
class MockOutputWebSocket {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    protected final BlockingQueue<JsonNode> messageQueue = new LinkedBlockingQueue<>();

    protected final WebSocket.Out<JsonNode> outputSocket = new WebSocket.Out<JsonNode>() {

        @Override
        public void write(JsonNode frame) { messageQueue.add(frame); }

        @Override
        public void close() {
            try { messageQueue.add(objectMapper.readTree("{\"closed\": true}")); }
            // This should not happen.
            catch (IOException e) { throw new RuntimeException(e); }
        }
    };

    public BlockingQueue<JsonNode> getMessageQueue() { return messageQueue; }

    public WebSocket.Out<JsonNode> getOutputSocket() { return outputSocket; }

}