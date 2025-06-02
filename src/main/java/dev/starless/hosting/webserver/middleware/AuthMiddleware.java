package dev.starless.hosting.webserver.middleware;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import dev.starless.hosting.webserver.jwt.JWTProvider;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class AuthMiddleware extends WebServerEndpoint {

    private final Set<String> protectedEndpoints = Set.of(
            "/api/auth/logout",
            "/api/account",
            "/api/upload",
            "/api/delete/",
            "/api/stats"
    );

    public AuthMiddleware(@NotNull WebServer server) {
        super(server, "*", HandlerType.BEFORE);
    }

    @Override
    public void handle(@NotNull Context ctx) {
        if (ctx.method().equals(HandlerType.OPTIONS)) {
            return;
        }

        final String path = ctx.path().toLowerCase();
        final boolean notProtected = this.protectedEndpoints
                .stream()
                .noneMatch(str -> path.startsWith(str.toLowerCase()));
        if (notProtected) {
            return;
        }

        final String session = ctx.cookie(SESSION_COOKIE_NAME);
        if (session == null) {
            this.getLogger().info("{} --> UNAUTHORIZED (no session cookie)", path);
            throw new UnauthorizedResponse();
        }

        final JsonElement element = JWTProvider.getInstance().verify(session);
        if (element == null) {
            this.getLogger().info("{} --> UNAUTHORIZED (invalid jwt)", path);
            ctx.removeCookie(SESSION_COOKIE_NAME);
            throw new UnauthorizedResponse();
        }

        final JsonObject payload = element.getAsJsonObject();
        final UserInfo info;
        try {
            info = this.server.getGson().fromJson(payload.getAsJsonObject("data"), UserInfo.class);
            ctx.attribute(SESSION_OBJECT_NAME, info);
        } catch (JsonSyntaxException ex) {
            ctx.removeCookie(SESSION_COOKIE_NAME);
            throw new BadRequestResponse();
        }
    }
}
