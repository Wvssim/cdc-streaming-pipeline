package ma.wvssim.ocr.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Ecoute le topic des evenements documents. Le consumer group qui lui est propre
 * ({@code ocr-service}) garantit qu'il recoit sa copie de chaque evenement (fan-out).
 */
@Component
public class DocumentEventListener {

    private final OcrService ocrService;

    public DocumentEventListener(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @KafkaListener(topics = "docs.public.documents", groupId = "ocr-service")
    public void onDocumentEvent(String rawEvent) {
        ocrService.extract(rawEvent);
    }
}
