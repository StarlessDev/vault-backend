package dev.starless.hosting.webserver;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.router.Endpoint;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Collections;

public abstract class WebServerEndpoint implements Handler {

    public static final String SESSION_COOKIE_NAME = "session";
    public static final String SESSION_OBJECT_NAME = "account";

    protected WebServer server;

    private final HandlerType method;
    private final String path;

    public WebServerEndpoint(@NotNull final WebServer server,
                             @NotNull final HandlerType method,
                             @NotNull final String path) {
        this.server = server;
        this.method = method;
        this.path = path;
    }

    protected Logger getLogger() {
        return server.getLogger();
    }

    protected void sendResponse(final Context ctx, final Response response) {
        ctx.status(response.statusCode());
        ctx.json(response);
    }

    protected void handleException(final Context ctx, final Throwable throwable) {
        final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.sendResponse(ctx, new Response()
                .statusCode(status)
                .error(throwable.getMessage())
        );
    }

    public Endpoint buildEndpoint() {
        return new Endpoint(method, path, Collections.emptySet(), this);
    }
}
