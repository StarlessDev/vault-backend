package dev.starless.hosting;

import dev.starless.hosting.config.Config;
import dev.starless.hosting.config.ConfigEntry;
import dev.starless.hosting.objects.EncryptionDetails;
import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.session.UserInfo;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public byte[] download(final String fileId, final String key)
            throws IOException,
            InvalidAlgorithmParameterException,
            NoSuchPaddingException,
            IllegalBlockSizeException,
            NoSuchAlgorithmException,
            InvalidKeySpecException,
            BadPaddingException,
            InvalidKeyException {
        final File file = basePath.resolve(fileId).toFile();
        if (file.exists()) {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return encryptionEngine.decrypt(bytes, key);
        } else {
            return null;
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
