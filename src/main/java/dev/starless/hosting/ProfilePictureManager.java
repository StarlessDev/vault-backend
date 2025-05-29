package dev.starless.hosting;

import dev.starless.hosting.config.Config;
import dev.starless.hosting.config.ConfigEntry;
import dev.starless.hosting.objects.session.UserInfo;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ProfilePictureManager {

    private final Logger logger = LoggerFactory.getLogger(ProfilePictureManager.class);

    private final Path basePath;

    public ProfilePictureManager(final Config config) {
        final File baseFile = new File(config.getString(ConfigEntry.PFP_MOUNT_POINT));
        if (!baseFile.isDirectory()) {
            logger.error("Invalid pfps mount directory: {}", baseFile.getAbsolutePath());
            System.exit(1);
        }
        this.basePath = baseFile.toPath();
    }

    public byte[] getImage(final UserInfo userInfo) {
        try {
            return Files.readAllBytes(this.getImagePath(userInfo));
        } catch (IOException e) {
            return null;
        }
    }

    public void removeImage(final UserInfo userInfo) {
        try {
            Files.delete(this.getImagePath(userInfo));
        } catch (IOException ignored) {
        }
    }

    public boolean saveImage(final UploadedFile file, final UserInfo userInfo) {
        try (final InputStream is = file.content()) {
            Files.copy(is, this.getImagePath(userInfo), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Path getImagePath(final UserInfo userInfo) {
        return basePath.resolve(String.valueOf(userInfo.id()));
    }
}
