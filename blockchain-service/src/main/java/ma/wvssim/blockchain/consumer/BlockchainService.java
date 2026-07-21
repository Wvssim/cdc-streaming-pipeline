package ma.wvssim.blockchain.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.wvssim.blockchain.domain.HashChain;
import ma.wvssim.blockchain.domain.HashChainRepository;
import ma.wvssim.blockchain.storage.StorageService;
import ma.wvssim.common.DebeziumEnvelope;
import ma.wvssim.common.DebeziumMessage;
import ma.wvssim.common.DocumentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Ajoute un maillon au registre d'integrite pour chaque document cree. Le chainage suppose un
 * ordre total sur les evenements : le topic tient sur une seule partition et {@code record} est
 * synchronise pour rester sequentiel meme si la concurrence du listener venait a changer.
 */
@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);
    private static final TypeReference<DebeziumMessage<DocumentPayload>> EVENT_TYPE = new TypeReference<>() {
    };

    private final HashChainRepository repository;
    private final StorageService storageService;
    // Cree ici : les records de common se deserialisent nativement, aucune config requise.
    private final ObjectMapper mapper = new ObjectMapper();

    public BlockchainService(HashChainRepository repository, StorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    /** Idempotent : une re-livraison (at-least-once) n'ajoute pas un second maillon pour le meme document. */
    @Transactional
    public synchronized void record(String rawEvent) {
        DebeziumEnvelope<DocumentPayload> envelope = parse(rawEvent);
        if (envelope == null || !envelope.isCreate() || envelope.after() == null) {
            return;
        }

        DocumentPayload doc = envelope.after();
        if (doc.id() == null || repository.existsByDocId(doc.id())) {
            return; // deja chaine
        }

        byte[] content = storageService.fetch(doc.storageKey());
        String docHash = HashUtil.sha256Hex(content);
        String prevHash = repository.findTopByOrderBySeqDesc()
                .map(HashChain::getChainHash)
                .orElse(HashUtil.GENESIS);
        String chainHash = HashUtil.chainHash(docHash, prevHash);

        repository.save(new HashChain(doc.id(), docHash, prevHash, chainHash, OffsetDateTime.now()));
        log.info("integrite : doc_id={} doc_hash={} chain_hash={}", doc.id(), docHash, chainHash);
    }

    /** Une erreur de parsing est une vraie anomalie : elle remonte au conteneur Kafka (-> retry puis DLT). */
    private DebeziumEnvelope<DocumentPayload> parse(String rawEvent) {
        try {
            return mapper.readValue(rawEvent, EVENT_TYPE).payload();
        } catch (Exception e) {
            throw new IllegalArgumentException("evenement illisible : " + e.getMessage(), e);
        }
    }
}
