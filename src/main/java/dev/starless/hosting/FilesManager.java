package dev.starless.hosting;

import dev.starless.hosting.config.Config;
import dev.starless.hosting.config.ConfigEntry;
import dev.starless.hosting.objects.EncryptionDetails;
import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.session.UserInfo;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class FilesManager {

    private final Logger logger = LoggerFactory.getLogger(FilesManager.class);

    private final Path basePath;
    private final EncryptionEngine encryptionEngine;

    public FilesManager(final Config config) {
        final File baseFile = new File(config.getString(ConfigEntry.FILES_MOUNT_POINT));
        this.encryptionEngine = new EncryptionEngine();
        if (!baseFile.isDirectory()) {
            logger.error("Invalid files mount directory: {}", baseFile.getAbsolutePath());
            System.exit(1);
        }
        this.basePath = baseFile.toPath();
    }

    public UserUpload encryptAndSave(final UserInfo info, final UploadedFile file) throws IOException {
        final UserUpload upload = new UserUpload(info.id(), file.filename());
        upload.size(file.size());

        try (final InputStream is = file.content();
             final FileOutputStream fos = new FileOutputStream(basePath.resolve(upload.fileId()).toFile())) {
            final EncryptionDetails encrypted = encryptionEngine.encrypt(is, fos);
            upload.key(encrypted.key());
            upload.ivAndSalt(encrypted.ivAndSalt());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException |
                 InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }

        return upload;
    }

    public void download(final Context ctx,
                         final UserUpload upload,
                         final String key) {
        final File file = basePath.resolve(upload.fileId()).toFile();
        if (!file.exists()) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        try {
            // Javalin will close the streams (hopefully...
            // it gets very angry when I close them)
            final CipherInputStream decryptedStream = encryptionEngine.decrypt(new FileInputStream(file), key);

            long encryptedSize = file.length();
            long decryptedSize = encryptedSize - (EncryptionEngine.GCM_TAG_LENGTH_BITS / 8);

            ctx.contentType(ContentType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + upload.fileName() + "\"")
                    .header("Content-Length", String.valueOf(decryptedSize))
                    .result(decryptedStream);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to download file: " + e.getMessage());
        }
    }

    public void delete(final String fileId) {
        try {
            Files.deleteIfExists(basePath.resolve(fileId));
        } catch (IOException e) {
            logger.error("Error deleting file: {}", fileId, e);
        }
    }
}
