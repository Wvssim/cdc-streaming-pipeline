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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/** Extrait le texte d'un document depose et le persiste (idempotent, dedup par doc_id). */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);
    private static final String ENGINE_TIKA = "tika";
    private static final String ENGINE_TESSERACT = "tesseract";
    private static final TypeReference<DebeziumMessage<DocumentPayload>> EVENT_TYPE = new TypeReference<>() {
    };

    private final ExtractedTextRepository repository;
    private final StorageService storageService;
    private final String tesseractDataPath;
    private final String tesseractLanguages;
    // Cree ici : les records de common se deserialisent nativement, aucune config requise.
    private final ObjectMapper mapper = new ObjectMapper();

    public OcrService(
            ExtractedTextRepository repository,
            StorageService storageService,
            @Value("${tesseract.datapath}") String tesseractDataPath,
            @Value("${tesseract.languages}") String tesseractLanguages) {
        this.repository = repository;
        this.storageService = storageService;
        this.tesseractDataPath = tesseractDataPath;
        this.tesseractLanguages = tesseractLanguages;
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
        boolean isImage = doc.contentType() != null && doc.contentType().startsWith("image/");
        String engine = isImage ? ENGINE_TESSERACT : ENGINE_TIKA;
        String text = isImage
                ? TesseractExtractor.extract(content, tesseractDataPath, tesseractLanguages)
                : TikaExtractor.extract(content);

        repository.save(new ExtractedText(doc.id(), text, engine, OffsetDateTime.now()));
        log.info("ocr : doc_id={} moteur={} caracteres_extraits={}", doc.id(), engine, text.length());
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
