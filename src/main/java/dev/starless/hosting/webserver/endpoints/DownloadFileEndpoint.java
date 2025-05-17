package dev.starless.hosting.webserver.endpoints;

import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.UserUpload_;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class DownloadFileEndpoint extends WebServerEndpoint {

    public DownloadFileEndpoint(@NotNull WebServer server) {
        super(server, HandlerType.GET, "/api/download/{fileId}");
    }

    @Override
    public void handle(@NotNull Context ctx) {
        final UserInfo user = ctx.sessionAttribute(SESSION_OBJECT_NAME);
        if (user == null) {
            throw new UnauthorizedResponse();
        }

        final String fileId = ctx.pathParam("fileId");
        final UserUpload upload;
        try (final Session session = server.getHibernate().getSessionFactory().openSession()) {
            final Transaction trx = session.beginTransaction();
            // Get object reference
            upload = session.find(UserUpload.class, fileId);
            if (upload == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }

            // Update last download information
            upload.onDownload();
            // Merge data
            session.merge(upload);
            // Commit changes to database
            trx.commit();
        }

        final byte[] bytes;
        try {
            // Read bytes from disk
            bytes = server.getFilesManager().download(fileId);
        } catch (IOException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        // Send file to client
        ctx.contentType(ContentType.OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + upload.fileName() + "\"")
                .header("Content-Length", String.valueOf(bytes.length))
                .result(bytes);
    }
}
