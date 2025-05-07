package dev.starless.hosting.config;

import dev.starless.hosting.templates.BackendConstants;
import lombok.Getter;

import java.util.Collections;

@Getter
public enum ConfigEntry {

    CONFIG_VERSION("config_version", BackendConstants.VERSION),

    DATABASE_URL("mariadb.url", "jdbc:mariadb://wsl/hibernate"),
    DATABASE_USER("mariadb.user", "root"),
    DATABASE_PASSWORD("mariadb.password", "password"),

    API_PORT("webserver.port", 8181),
    ALLOWED_DOMAINS("webserver.allowed_domains", Collections.emptyList()),
    JWT_SECRET("webserver.jwt_secret", ""),

    OAUTH_CLIENT_ID("webserver.oauth.client_id", "oauth-client-id"),
    OAUTH_CLIENT_SECRET("webserver.oauth.client_secret", "oauth-client-secret"),
    OAUTH_REDIRECT_URI("webserver.oauth.redirect_uri", "https://example.com"),

    FILES_MOUNT_POINT("uploads.mount_point", "./uploads");

    private final String path;
    private final Object defaultValue;

    ConfigEntry(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }
}

