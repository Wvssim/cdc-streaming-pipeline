package ma.wvssim.ocr.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.wvssim.common.DebeziumEnvelope;
import ma.wvssim.common.DebeziumMessage;
import ma.wvssim.common.DocumentPayload;
import ma.wvssim.ocr.domain.ExtractedText;
import ma.wvssim.ocr.domain.ExtractedTextRepository;
import ma.wvssim.ocr.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/** Extrait le texte d'un document depose et le persiste (idempotent, dedup par doc_id). */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);
    private static final String ENGINE = "tika";
    private static final TypeReference<DebeziumMessage<DocumentPayload>> EVENT_TYPE = new TypeReference<>() {
    };

    private final ExtractedTextRepository repository;
    private final StorageService storageService;
    // Cree ici : les records de common se deserialisent nativement, aucune config requise.
    private final ObjectMapper mapper = new ObjectMapper();

    public OcrService(ExtractedTextRepository repository, StorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    /** Idempotent : une re-livraison (at-least-once) n'extrait pas deux fois le meme document. */
    @Transactional
    public void extract(String rawEvent) {
        DebeziumEnvelope<DocumentPayload> envelope = parse(rawEvent);
        if (envelope == null || !envelope.isCreate() || envelope.after() == null) {
            return;
        }

        DocumentPayload doc = envelope.after();
        if (doc.id() == null || repository.existsById(doc.id())) {
            return; // deja extrait
        }

        byte[] content = storageService.fetch(doc.storageKey());
        String text = TikaExtractor.extract(content);

        repository.save(new ExtractedText(doc.id(), text, ENGINE, OffsetDateTime.now()));
        log.info("ocr : doc_id={} caracteres_extraits={}", doc.id(), text.length());
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
