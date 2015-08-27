package models.enums;

/**
 * Created by richard on 27.08.15.
 */
public enum OAuth2ErrorCodes {
    INVALID_REQUEST("invalid_request"),
    INVALID_CLIENT("invalid_client"),
    UNAUTHORIZED_CLIENT("unauthorized_client"),
    REDIRECT_URI_MISMATCH("redirect_uri_mismatch"),
    ACCESS_DENIED("access_denied"),
    UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type"),
    INVALID_SCOPE("invalid_scope")
    ;


    private String identifier;

    OAuth2ErrorCodes(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
