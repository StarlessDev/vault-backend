package dev.starless.hosting.webserver.endpoints.account.avatar;

import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.*;
import org.jetbrains.annotations.NotNull;

public class AvatarEndpoint extends WebServerEndpoint {

    public AvatarEndpoint(@NotNull WebServer server) {
        super(server, "/api/account/avatar", HandlerType.GET);
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        final UserInfo info = ctx.attribute(SESSION_OBJECT_NAME);
        if (info == null) {
            throw new UnauthorizedResponse();
        }

        final byte[] image = this.server.getPfpManager().getImage(info);
        if (image == null) {
            throw new NotFoundResponse();
        }
        final String mimeType = server.getTika().detect(image);

        ctx.status(HttpStatus.OK)
                .contentType(mimeType)
                .result(image);
    }
}
