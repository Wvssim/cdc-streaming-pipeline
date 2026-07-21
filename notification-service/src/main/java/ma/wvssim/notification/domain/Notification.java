package ma.wvssim.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Un e-mail envoye pour un document (table {@code notif.notifications}). */
@Entity
@Table(name = "notifications", schema = "notif")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id")
    private Long docId;

    private String recipient;

    private String status;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    protected Notification() {
        // requis par JPA
    }

    public Notification(Long docId, String recipient, String status, OffsetDateTime sentAt) {
        this.docId = docId;
        this.recipient = recipient;
        this.status = status;
        this.sentAt = sentAt;
    }

    public Long getId() {
        return id;
    }

    public Long getDocId() {
        return docId;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }
}
