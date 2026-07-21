package ma.wvssim.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Une trace d'audit (table {@code audit.audit_log}). */
@Entity
@Table(name = "audit_log", schema = "audit")
public class AuditLog {

    /** Cle d'idempotence : identifiant unique de l'evenement CDC (base sur le LSN). */
    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "doc_id")
    private Long docId;

    private String action;

    private String actor;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;

    protected AuditLog() {
        // requis par JPA
    }

    public AuditLog(String eventId, Long docId, String action, String actor, OffsetDateTime occurredAt) {
        this.eventId = eventId;
        this.docId = docId;
        this.action = action;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public String getEventId() {
        return eventId;
    }

    public Long getDocId() {
        return docId;
    }

    public String getAction() {
        return action;
    }

    public String getActor() {
        return actor;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
