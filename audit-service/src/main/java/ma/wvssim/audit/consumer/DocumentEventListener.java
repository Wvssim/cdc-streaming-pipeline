package ma.wvssim.audit.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Ecoute le topic des evenements documents. Le consumer group qui lui est propre
 * ({@code audit-service}) garantit qu'audit recoit sa copie de chaque evenement (fan-out).
 */
@Component
public class DocumentEventListener {

    private final AuditService auditService;

    public DocumentEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(topics = "docs.public.documents", groupId = "audit-service")
    public void onDocumentEvent(String rawEvent) {
        auditService.record(rawEvent);
    }
}
