package dev.starless.hosting.webserver.endpoints.file;

import com.google.gson.JsonObject;
import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.Response;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
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

        final JsonObject obj = new JsonObject();
        obj.addProperty("fileId", upload.fileId());
        obj.addProperty("fileName", upload.fileName());
        obj.addProperty("uploadDate", upload.uploadDate().toEpochMilli());

        final UserInfo user = ctx.attribute(SESSION_OBJECT_NAME);
        if (user != null && user.id() == upload.uploaderId()) {
            obj.addProperty("totalDownloads", upload.totalDownloads());
            obj.addProperty("uploaderId",  upload.uploaderId());
            obj.addProperty("lastDownload", upload.lastDownload().toEpochMilli());
        }

        ctx.json(obj);
    }
}
