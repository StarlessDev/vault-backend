package dev.starless.hosting.webserver.endpoints.account.avatar;

import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import org.jetbrains.annotations.NotNull;

public class DeleteAvatarEndpoint extends WebServerEndpoint {

    public DeleteAvatarEndpoint(@NotNull WebServer server) {
        super(server, "/api/account/avatar", HandlerType.DELETE);
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        if (ctx.method().equals(HandlerType.OPTIONS)) {
            ctx.status(HttpStatus.OK);
            return;
        }

        final UserInfo info = ctx.attribute(SESSION_OBJECT_NAME);
        if (info == null) {
            throw new UnauthorizedResponse();
        }

        server.getPfpManager().removeImage(info);
        ctx.status(HttpStatus.OK);
    }
}
