package models.base;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Interface for objects, that are serializable to ObjectNode instances
 */
public interface IJsonNodeSerializable {
    /**
     * Serializes this object as JsonNode.
     *
     * @return ObjectNode instance
     */
    public ObjectNode getAsJson();
}
