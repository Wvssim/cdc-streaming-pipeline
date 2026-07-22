package ma.wvssim.siem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Une alerte levee par le moteur de regles (table {@code siem.alerts}). */
@Entity
@Table(name = "alerts", schema = "siem")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id")
    private Long docId;

    private String rule;

    private String severity;

    private String detail;

    @Column(name = "raised_at")
    private OffsetDateTime raisedAt;

    protected Alert() {
        // requis par JPA
    }

    public Alert(Long docId, String rule, String severity, String detail, OffsetDateTime raisedAt) {
        this.docId = docId;
        this.rule = rule;
        this.severity = severity;
        this.detail = detail;
        this.raisedAt = raisedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getDocId() {
        return docId;
    }

    public String getRule() {
        return rule;
    }

    public String getSeverity() {
        return severity;
    }

    public String getDetail() {
        return detail;
    }

    public OffsetDateTime getRaisedAt() {
        return raisedAt;
    }
}
