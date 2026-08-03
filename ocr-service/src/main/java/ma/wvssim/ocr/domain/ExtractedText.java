package ma.wvssim.ocr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Texte extrait d'un document (table {@code ocr.extracted_text}), une ligne par document. */
@Entity
@Table(name = "extracted_text", schema = "ocr")
public class ExtractedText {

    @Id
    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "text")
    private String text;

    @Column(name = "engine")
    private String engine;

    @Column(name = "extracted_at")
    private OffsetDateTime extractedAt;

    protected ExtractedText() {
        // requis par JPA
    }

    public ExtractedText(Long docId, String text, String engine, OffsetDateTime extractedAt) {
        this.docId = docId;
        this.text = text;
        this.engine = engine;
        this.extractedAt = extractedAt;
    }

    public Long getDocId() {
        return docId;
    }

    public String getText() {
        return text;
    }

    public String getEngine() {
        return engine;
    }

    public OffsetDateTime getExtractedAt() {
        return extractedAt;
    }
}
