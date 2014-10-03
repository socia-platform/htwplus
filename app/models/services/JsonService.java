package models.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.base.IJsonNodeSerializable;
import play.Logger;
import play.libs.Json;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This service provides helper methods for JSON purposes.
 */
@SuppressWarnings("unused")
public class JsonService {
    /**
     * Singleton instance
     */
    private static JsonService instance = null;

    /**
     * Private constructor for singleton instance
     */
    private JsonService() { }

    /**
     * Returns the singleton instance.
     *
     * @return NotificationHandler instance
     */
    public static JsonService getInstance() {
        if (JsonService.instance == null) {
            JsonService.instance = new JsonService();
        }

        return JsonService.instance;
    }

    /**
     * Returns a JsonNode from a JSON String.
     *
     * @param jsonString JSON String
     * @return JsonNode instance
     */
    public JsonNode getJsonFromString(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readTree(jsonString);
        } catch (Throwable throwable) {
            Logger.error("Error while creating JSON: " + throwable.getMessage());
            return null;
        }
    }

    /**
     * Returns an ObjectNode instance from a map.
     *
     * @param map Map instance
     * @return ObjectNode instance
     */
    public ObjectNode getObjectNodeFromMap(Map<String, Object> map) {
        ObjectNode node = Json.newObject();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value.getClass().equals(String.class)) {
                node.put(entry.getKey(), (String)value);
            } else if (value.getClass().equals(Integer.class)) {
                node.put(entry.getKey(), (Integer)value);
            } else if (value.getClass().equals(Long.class)) {
                node.put(entry.getKey(), (Long)value);
            } else if (value.getClass().equals(Date.class)) {
                node.put(entry.getKey(), ((Date)value).getTime());
            } else {
                node.put(entry.getKey(), value.toString());
            }
        }

        return node;
    }

    /**
     * Returns a List of ObjectNode instances by a list of IJsonNodeSerializable implementing instances.
     *
     * @param serializableList List of IJsonNodeSerializable implementing instances
     * @return List of ObjectNode instances
     */
    public List<ObjectNode> getJsonList(List<? extends IJsonNodeSerializable> serializableList) {
        List<ObjectNode> jsonList = new ArrayList<>(serializableList.size());
        for (IJsonNodeSerializable serializable : serializableList) {
            jsonList.add(serializable.getAsJson());
        }

        return jsonList;
    }
}
