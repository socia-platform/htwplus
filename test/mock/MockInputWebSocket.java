package mock;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;
import play.mvc.WebSocket;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Credits: http://vlkan.com/blog/post/2014/08/07/testing-websockets-in-play/
 */
@ThreadSafe
class MockInputWebSocket {
    protected final List<F.Callback<JsonNode>> messageListeners = Collections.synchronizedList(new ArrayList<F.Callback<JsonNode>>());
    protected final List<F.Callback0> closeListeners = Collections.synchronizedList(new ArrayList<F.Callback0>());
    protected final WebSocket.In<JsonNode> inputSocket = new WebSocket.In<JsonNode>() {
        @Override
        public void onMessage(F.Callback<JsonNode> callback) { messageListeners.add(callback); }

        @Override
        public void onClose(F.Callback0 callback) { closeListeners.add(callback); }

    };

    public void write(JsonNode data) throws Throwable {
        for (F.Callback<JsonNode> listener : messageListeners)
            listener.invoke(data);
    }

    public void close() throws Throwable {
        for (F.Callback0 listener : closeListeners)
            listener.invoke();
    }

    public WebSocket.In<JsonNode> getInputSocket() { return inputSocket; }
}
