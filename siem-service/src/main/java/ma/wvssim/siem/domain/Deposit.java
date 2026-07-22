package ma.wvssim.siem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Historique interne des depots deja vus (table {@code siem.deposits}), alimente uniquement par
 * les evenements Kafka consommes. Necessaire pour la regle de frequence : siem-service ne lit
 * jamais le schema {@code public} d'un autre service (isolation logique entre services).
 */
@Entity
@Table(name = "deposits", schema = "siem")
public class Deposit {

    /** Cle d'idempotence : identifiant du document (une seule creation par document dans ce MVP). */
    @Id
    @Column(name = "doc_id")
    private Long docId;

    private String actor;

    private String filename;

    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;

    protected Deposit() {
        // requis par JPA
    }

    public Deposit(Long docId, String actor, String filename, OffsetDateTime uploadedAt) {
        this.docId = docId;
        this.actor = actor;
        this.filename = filename;
        this.uploadedAt = uploadedAt;
    }

    public Long getDocId() {
        return docId;
    }

    public String getActor() {
        return actor;
    }

    public String getFilename() {
        return filename;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }
}
