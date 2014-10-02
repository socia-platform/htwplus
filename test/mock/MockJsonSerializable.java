package mock;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.base.IJsonNodeSerializable;
import play.libs.Json;

import java.util.Date;
import java.util.Random;

/**
 * Mock for JsonSerializable interface.
 */
public class MockJsonSerializable implements IJsonNodeSerializable {
    @Override
    public ObjectNode getAsJson() {
        ObjectNode node = Json.newObject();
        node.put("class_name", this.getClass().getSimpleName());
        node.put("time", (new Date()).getTime());
        node.put("random", (new Random()).nextInt(100));

        return node;
    }
}
