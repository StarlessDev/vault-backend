package dev.starless.hosting.webserver.endpoints.file;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class DownloadFileEndpoint extends WebServerEndpoint {

    public DownloadFileEndpoint(@NotNull WebServer server) {
        super(server, HandlerType.POST, "/api/download/{fileId}");
    }

    @Override
    public void handle(@NotNull Context ctx) {
        final UserInfo user = ctx.attribute(SESSION_OBJECT_NAME);
        if (user == null) {
            throw new UnauthorizedResponse();
        }

        final String key;
        try {
            final JsonObject obj = JsonParser.parseString(ctx.body()).getAsJsonObject();
            key = obj.get("key").getAsString();
        } catch (JsonSyntaxException | UnsupportedOperationException | IllegalStateException ex) {
            throw new BadRequestResponse();
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
            bytes = server.getFilesManager().download(fileId, key);
        } catch (IOException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                 | InvalidKeySpecException | BadPaddingException | InvalidKeyException e) {
            throw new InternalServerErrorResponse("Could not decrypt file!");
        }
        // Send file to client
        ctx.contentType(ContentType.OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + upload.fileName() + "\"")
                .header("Content-Length", String.valueOf(bytes.length))
                .result(bytes);
    }
}
