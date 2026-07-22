package ma.wvssim.siem.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Ecoute le topic des evenements documents. Le consumer group qui lui est propre
 * ({@code siem-service}) garantit qu'il recoit sa copie de chaque evenement (fan-out).
 */
@Component
public class DocumentEventListener {

    private final SiemService siemService;

    public DocumentEventListener(SiemService siemService) {
        this.siemService = siemService;
    }

    @KafkaListener(topics = "docs.public.documents", groupId = "siem-service")
    public void onDocumentEvent(String rawEvent) {
        siemService.evaluate(rawEvent);
    }
}
