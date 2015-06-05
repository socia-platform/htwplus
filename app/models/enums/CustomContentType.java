package models.enums;

/**
 * Created by richard on 04.06.15.
 */
public enum CustomContentType {
    JSON_COLLECTION("application/vnd.collection+json");

    private String identifier;

    CustomContentType(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
