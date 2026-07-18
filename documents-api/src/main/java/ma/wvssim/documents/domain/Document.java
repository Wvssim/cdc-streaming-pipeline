package ma.wvssim.documents.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Metadonnees d'un document (table source {@code public.documents}, captee par Debezium). */
@Entity
@Table(name = "documents", schema = "public")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    protected Document() {
        // requis par JPA
    }

    public Document(String filename, String contentType, Long size,
                    String storageKey, String uploadedBy, OffsetDateTime uploadedAt) {
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.storageKey = storageKey;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSize() {
        return size;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }
}
