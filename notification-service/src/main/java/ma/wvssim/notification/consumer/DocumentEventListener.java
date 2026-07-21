package ma.wvssim.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Ecoute le topic des evenements documents. Le consumer group qui lui est propre
 * ({@code notification-service}) garantit qu'il recoit sa copie de chaque evenement (fan-out).
 */
@Component
public class DocumentEventListener {

    private final NotificationService notificationService;

    public DocumentEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "docs.public.documents", groupId = "notification-service")
    public void onDocumentEvent(String rawEvent) {
        notificationService.notify(rawEvent);
    }
}
