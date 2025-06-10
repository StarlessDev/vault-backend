package dev.starless.hosting.webserver.endpoints.file;

import com.google.gson.JsonObject;
import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.Response;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

public class FileInfoEndpoint extends WebServerEndpoint {

    public FileInfoEndpoint(@NotNull WebServer server) {
        super(server, "/api/file/{fileId}", HandlerType.GET);
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        final String fileId = ctx.pathParam("fileId");
        final UserUpload upload;
        try (final Session session = server.getHibernate().getSessionFactory().openSession()) {
            upload = session.find(UserUpload.class, fileId);
        }

        if (upload == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        final UserInfo user = ctx.attribute(SESSION_OBJECT_NAME);
        final JsonObject obj = upload.toJson(user);
        ctx.json(obj);
    }
}
