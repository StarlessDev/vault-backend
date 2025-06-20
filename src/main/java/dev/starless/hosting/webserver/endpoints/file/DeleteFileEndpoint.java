package dev.starless.hosting.webserver.endpoints.file;

import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class DeleteFileEndpoint extends WebServerEndpoint {

    public DeleteFileEndpoint(@NotNull WebServer server) {
        super(server, "/api/delete/{fileId}", HandlerType.DELETE, HandlerType.OPTIONS);
    }

    @Override
    public void handle(@NotNull Context ctx) {
        if (ctx.method().equals(HandlerType.OPTIONS)) {
            ctx.status(HttpStatus.OK);
            return;
        }

        final UserInfo user = ctx.attribute(SESSION_OBJECT_NAME);
        if (user == null) {
            throw new UnauthorizedResponse();
        }

        final String fileId = ctx.pathParam("fileId");
        // Delete from database
        final AtomicBoolean notFound = new AtomicBoolean(false);
        server.getHibernate().getSessionFactory().inTransaction(session -> {
            final UserUpload upload = session.find(UserUpload.class, fileId);
            if (upload == null) {
                notFound.set(true);
                return;
            }
            session.remove(upload);
        });

        if (notFound.get()) {
            ctx.status(HttpStatus.NOT_FOUND);
        } else {
            // Delete file from disk
            server.getFilesManager().delete(fileId);
            // Send OK
            ctx.status(HttpStatus.OK);
        }
    }
}
