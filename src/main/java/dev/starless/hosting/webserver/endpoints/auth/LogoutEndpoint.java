package dev.starless.hosting.webserver.endpoints.auth;

import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import org.jetbrains.annotations.NotNull;

public class LogoutEndpoint extends WebServerEndpoint {

    public LogoutEndpoint(@NotNull WebServer server) {
        super(server, "/api/auth/logout", HandlerType.POST);
    }

    @Override
    public void handle(@NotNull Context ctx) {
        final UserInfo info = ctx.attribute(SESSION_OBJECT_NAME);
        if (info == null) {
            throw new UnauthorizedResponse();
        }

        ctx.removeCookie(SESSION_COOKIE_NAME);
    }
}
