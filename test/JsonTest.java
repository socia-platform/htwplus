import com.fasterxml.jackson.databind.node.ObjectNode;
import mock.MockJsonSerializable;
import models.services.JsonService;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
/**clea
 * Tests for JsonService.
 */
public class JsonTest {
    /**
     * Test JsonService method getObjectNodeFromMap()
     */
    @Test
    public void testGetObjectNodeFromMap() {
        Date date = new Date();
        Map<String, Object> map = new HashMap<>();
        map.put("String", "Hello World");
        map.put("Integer", 1000);
        map.put("Long", 2000L);
        map.put("Date", date);
        map.put("Object", new Object());

        ObjectNode testNode = JsonService.getInstance().getObjectNodeFromMap(map);
        assertNotNull(testNode);
//        assertThat(testNode).isNotNull();
//        assertThat(testNode.has("String")).isTrue();
//        assertThat(testNode.get("String").asText()).contains("Hello World");
//        assertThat(testNode.has("Integer")).isTrue();
//        assertThat(testNode.get("Integer").asInt()).isEqualTo(1000);
//        assertThat(testNode.has("Long")).isTrue();
//        assertThat(testNode.get("Long").asLong()).isEqualTo(2000L);
//        assertThat(testNode.has("Date")).isTrue();
//        assertThat(testNode.get("Date").asLong()).isEqualTo(date.getTime());
//        assertThat(testNode.has("Object")).isTrue();
//        assertThat(testNode.get("Object").asText()).contains("java.lang.Object");
//        assertThat(testNode.has("Nothing")).isFalse();
    }

    /**
     * Test JsonService method getJsonList()
     */
    @Test
    public void testGetJsonList() {
        List<MockJsonSerializable> list = new ArrayList<>();
        list.add(new MockJsonSerializable());
        list.add(new MockJsonSerializable());
        list.add(new MockJsonSerializable());
        list.add(new MockJsonSerializable());
        list.add(new MockJsonSerializable());

        List<ObjectNode> testList = JsonService.getInstance().getJsonList(list);
//        assertThat(testList).isNotNull();
//        assertThat(testList.size()).isEqualTo(list.size());
//        for (ObjectNode node : testList) {
//            assertThat(node.has("class_name")).isTrue();
//            assertThat(node.has("time")).isTrue();
//            assertThat(node.has("random")).isTrue();
//            assertThat(node.has("nothing")).isFalse();
//            assertThat(node.get("class_name").asText()).contains("MockJsonSerializable");
//        }
    }
}
