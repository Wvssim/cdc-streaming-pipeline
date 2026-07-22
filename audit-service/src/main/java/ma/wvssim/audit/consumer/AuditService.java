package ma.wvssim.audit.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.wvssim.audit.domain.AuditLog;
import ma.wvssim.audit.domain.AuditLogRepository;
import ma.wvssim.common.DebeziumEnvelope;
import ma.wvssim.common.DebeziumMessage;
import ma.wvssim.common.DocumentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/** Transforme un evenement CDC (topic documents) en ligne d'audit. */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final TypeReference<DebeziumMessage<DocumentPayload>> EVENT_TYPE = new TypeReference<>() {
    };

    private final AuditLogRepository repository;
    // Cree ici : les records de common se deserialisent nativement, aucune config requise.
    private final ObjectMapper mapper = new ObjectMapper();

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Idempotent : une re-livraison (at-least-once) ne cree pas de doublon, grace a l'event_id. */
    @Transactional
    public void record(String rawEvent) {
        DebeziumEnvelope<DocumentPayload> envelope = parse(rawEvent);
        if (envelope == null || envelope.op() == null) {
            return;
        }

        String eventId = eventId(envelope);
        if (repository.existsById(eventId)) {
            return; // deja trace
        }

        DocumentPayload row = envelope.isDelete() ? envelope.before() : envelope.after();
        Long docId = row != null ? row.id() : null;
        String actor = row != null ? row.uploadedBy() : null;
        String action = AuditActions.of(envelope.op());

        repository.save(new AuditLog(eventId, docId, action, actor, OffsetDateTime.now()));
        log.info("audit : event_id={} action={} doc_id={} actor={}", eventId, action, docId, actor);
    }

    /** Une erreur de parsing est une vraie anomalie : elle remonte au conteneur Kafka (-> retry puis DLT). */
    private DebeziumEnvelope<DocumentPayload> parse(String rawEvent) {
        try {
            return mapper.readValue(rawEvent, EVENT_TYPE).payload();
        } catch (Exception e) {
            throw new IllegalArgumentException("evenement illisible : " + e.getMessage(), e);
        }
    }

    /** Le LSN identifie de facon unique un changement dans le WAL PostgreSQL. */
    private String eventId(DebeziumEnvelope<DocumentPayload> envelope) {
        DebeziumEnvelope.Source source = envelope.source();
        if (source != null && source.lsn() != null) {
            return "lsn-" + source.lsn();
        }
        DocumentPayload row = envelope.after() != null ? envelope.after() : envelope.before();
        Long id = row != null ? row.id() : null;
        return envelope.op() + "-" + id + "-" + envelope.tsMs();
    }
}
