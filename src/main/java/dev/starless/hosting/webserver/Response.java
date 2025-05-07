package dev.starless.hosting.webserver;

import io.javalin.http.HttpStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true, chain = true)
public class Response {

    public static Response ok() {
        return new Response();
    }

    public static Response badRequest(@Nullable final String message) {
        return new Response()
                .statusCode(HttpStatus.BAD_REQUEST)
                .error(message);
    }

    public static Response serverError(@Nullable final String message) {
        return new Response().error(message);
    }

    private @Setter(AccessLevel.NONE) final Map<String, Object> values;

    private Outcome outcome;
    private HttpStatus statusCode;
    private String errorMessage;

    public Response() {
        this.values = new HashMap<>();

        this.statusCode = HttpStatus.OK;
        this.outcome = Outcome.SUCCESS;
        this.errorMessage = null;
    }

    public Response addData(final String key, final Object value) {
        this.values.put(key, value);
        return this;
    }

    public Response removeData(final String key) {
        this.values.remove(key);
        return this;
    }

    public Response error(final String errorMessage) {
        this.outcome = Outcome.ERROR;
        this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorMessage = errorMessage;
        return this;
    }

    public enum Outcome {
        SUCCESS,
        ERROR
    }
}
