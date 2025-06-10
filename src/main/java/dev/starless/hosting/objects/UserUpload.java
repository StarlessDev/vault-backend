package dev.starless.hosting.objects;

import com.google.gson.JsonObject;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.utils.RandomUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Accessors(fluent = true)

@Table(name = "Uploads")
@Entity
public class UserUpload {

    @Id
    private String fileId;
    @Column(nullable = false)
    private int uploaderId;
    @Setter
    @Column(nullable = false)
    private String fileName;
    @Column(nullable = false)
    private Instant uploadDate;
    @Column(nullable = false)
    @ColumnDefault("0")
    private long size;

    @Column(nullable = false)
    private long totalDownloads;
    @Setter
    @Column(nullable = false)
    private Instant lastDownload;

    private transient String key, ivAndSalt;

    public UserUpload(int uploaderId, String fileName) {
        this.fileId = RandomUtils.randomString(8);
        this.uploaderId = uploaderId;
        this.fileName = fileName;
        this.uploadDate = Instant.now();

        this.totalDownloads = 0L;
        this.lastDownload = Instant.EPOCH;
    }

    public void onDownload() {
        this.totalDownloads++;
        this.lastDownload = Instant.now();
    }

    public JsonObject toJson(@Nullable final UserInfo userRequester) {
        final JsonObject obj = new JsonObject();
        obj.addProperty("fileId", this.fileId());
        obj.addProperty("fileName", this.fileName());
        obj.addProperty("uploadDate", this.uploadDate().toEpochMilli());
        obj.addProperty("size", this.size());

        // Display statistics only to the file uploader
        if (userRequester != null && userRequester.id() == this.uploaderId()) {
            obj.addProperty("totalDownloads", this.totalDownloads());
            obj.addProperty("uploaderId",  this.uploaderId());
            obj.addProperty("lastDownload", this.lastDownload().toEpochMilli());
        }

        return obj;
    }
}
