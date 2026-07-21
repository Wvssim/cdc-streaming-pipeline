package ma.wvssim.blockchain.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Ecoute le topic des evenements documents. Le consumer group qui lui est propre
 * ({@code blockchain-service}) garantit qu'il recoit sa copie de chaque evenement (fan-out).
 */
@Component
public class DocumentEventListener {

    private final BlockchainService blockchainService;

    public DocumentEventListener(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @KafkaListener(topics = "docs.public.documents", groupId = "blockchain-service")
    public void onDocumentEvent(String rawEvent) {
        blockchainService.record(rawEvent);
    }
}
