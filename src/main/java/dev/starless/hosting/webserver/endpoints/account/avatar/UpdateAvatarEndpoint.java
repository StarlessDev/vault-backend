package dev.starless.hosting.webserver.endpoints.account.avatar;

import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.Response;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public class UpdateAvatarEndpoint extends WebServerEndpoint {

    public UpdateAvatarEndpoint(@NotNull WebServer server) {
        super(server, "/api/account/avatar", HandlerType.POST);
    }

    @Override
    public void handle(@NotNull Context ctx) {
        final UserInfo info = ctx.attribute(SESSION_OBJECT_NAME);
        if (info == null) {
            throw new UnauthorizedResponse();
        }

        final UploadedFile file = ctx.uploadedFile("image");
        if (file == null) {
            throw new BadRequestResponse();
        }

        // The performance impact of detecting file types is low
        final String mimeType;
        try (InputStream is = file.content()) {
            mimeType = server.getTika().detect(is, file.filename());
        } catch (IOException e) {
            this.sendResponse(ctx, Response.badRequest("The file is not an image"));
            return;
        }

        if (mimeType == null || !mimeType.startsWith("image/")) {
            this.sendResponse(ctx, Response.badRequest("The file is not an image"));
            return;
        }

        if (this.server.getPfpManager().saveImage(file, info)) {
            ctx.status(HttpStatus.OK);
        } else {
            this.sendResponse(ctx, Response.serverError("Could not update profile picture."));
        }
    }
}
