package dev.starless.hosting.objects;

import dev.starless.hosting.utils.RandomUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

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
    private long totalDownloads;
    @Setter
    @Column(nullable = false)
    private Instant lastDownload;

    private transient String key, salt, iv;

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
}
