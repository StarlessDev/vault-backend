package dev.starless.hosting.webserver.endpoints.file;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.starless.hosting.config.ConfigEntry;
import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.UploadedFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UploadFileEndpoint extends WebServerEndpoint {

    public UploadFileEndpoint(@NotNull WebServer server) {
        super(server, "/api/upload", HandlerType.POST);
    }

    @Override
    public void handle(@NotNull Context ctx) {
        final UserInfo info = ctx.attribute(SESSION_OBJECT_NAME);
        if (info == null) {
            throw new UnauthorizedResponse();
        }

        final List<UploadedFile> files = ctx.uploadedFiles();
        final List<FailedUpload> failedUploads = new ArrayList<>();
        final List<UserUpload> uploads = new ArrayList<>();
        for (UploadedFile file : files) {
            if (file.size() >= server.getConfig().getInt(ConfigEntry.FILES_MAX_SIZE)) {
                failedUploads.add(new FailedUpload(file.filename(), "The file is too big"));
                continue;
            }

            this.server.getLogger().info("working with {}", file.filename());
            try {
                final UserUpload upload = this.server.getFilesManager().encryptAndSave(info, file);
                uploads.add(upload);
            } catch (IOException e) {
                failedUploads.add(new FailedUpload(file.filename(), e.getMessage()));
                this.server.getLogger().error("Error uploading file: {}", file.filename(), e);
            }
        }

        // Update database
        this.addUploadsToDatabase(uploads);

        // Send response
        final JsonArray array = this.buildResponse(uploads);
        failedUploads.forEach(failedUpload -> {
            final JsonObject obj = new JsonObject();
            obj.addProperty("filename", failedUpload.filename);
            obj.addProperty("error", failedUpload.error);
            array.add(obj);
        });
        ctx.json(array);
    }

    private void addUploadsToDatabase(final List<UserUpload> uploads) {
        server.getHibernate().getSessionFactory().inTransaction(session -> {
            uploads.forEach(session::persist);
        });
    }

    private JsonArray buildResponse(final List<UserUpload> uploads) {
        final JsonArray array = new JsonArray();
        uploads.forEach(upload -> {
            final String fullKey = upload.key() + upload.ivAndSalt();
            final JsonObject uploadPayload = new JsonObject();
            uploadPayload.addProperty("username", upload.fileName());
            uploadPayload.addProperty("id", upload.fileId());
            uploadPayload.addProperty("fullKey", URLEncoder.encode(fullKey, StandardCharsets.UTF_8));
            array.add(uploadPayload);
        });
        return array;
    }

    record FailedUpload(String filename, String error) {
    }
}
