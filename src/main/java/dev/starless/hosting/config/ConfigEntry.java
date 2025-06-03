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

    FILES_MOUNT_POINT("uploads.mount_point", "./uploads"),
    FILES_MAX_SIZE("uploads.max_size", 5E7),
    PFP_MOUNT_POINT("pfp.mount_point", "./pfps"),;

    private final String path;
    private final Object defaultValue;

    ConfigEntry(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }
}

