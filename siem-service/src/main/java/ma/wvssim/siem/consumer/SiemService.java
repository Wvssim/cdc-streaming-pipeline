package ma.wvssim.siem.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.wvssim.common.DebeziumEnvelope;
import ma.wvssim.common.DebeziumMessage;
import ma.wvssim.common.DocumentPayload;
import ma.wvssim.siem.domain.Alert;
import ma.wvssim.siem.domain.AlertRepository;
import ma.wvssim.siem.domain.Deposit;
import ma.wvssim.siem.domain.DepositRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/** Applique les 3 regles de detection a chaque document cree. */
@Service
public class SiemService {

    private static final Logger log = LoggerFactory.getLogger(SiemService.class);
    private static final TypeReference<DebeziumMessage<DocumentPayload>> EVENT_TYPE = new TypeReference<>() {
    };

    private final DepositRepository depositRepository;
    private final AlertRepository alertRepository;
    private final SiemRulesProperties rules;
    private final Set<String> suspiciousExtensions;
    // Cree ici : les records de common se deserialisent nativement, aucune config requise.
    private final ObjectMapper mapper = new ObjectMapper();

    public SiemService(DepositRepository depositRepository, AlertRepository alertRepository,
                        SiemRulesProperties rules) {
        this.depositRepository = depositRepository;
        this.alertRepository = alertRepository;
        this.rules = rules;
        this.suspiciousExtensions = new HashSet<>(rules.suspiciousExtensions());
    }

    /** Idempotent : une re-livraison (at-least-once) ne reevalue pas deux fois le meme document. */
    @Transactional
    public void evaluate(String rawEvent) {
        DebeziumEnvelope<DocumentPayload> envelope = parse(rawEvent);
        if (envelope == null || !envelope.isCreate() || envelope.after() == null) {
            return;
        }

        DocumentPayload doc = envelope.after();
        if (doc.id() == null || depositRepository.existsById(doc.id())) {
            return; // deja evalue
        }

        OffsetDateTime uploadedAt = parseUploadedAt(doc.uploadedAt());
        depositRepository.save(new Deposit(doc.id(), doc.uploadedBy(), doc.filename(), uploadedAt));

        checkFrequency(doc, uploadedAt);
        checkBusinessHours(doc, uploadedAt);
        checkExtension(doc);
    }

    private void checkFrequency(DocumentPayload doc, OffsetDateTime uploadedAt) {
        OffsetDateTime windowStart = uploadedAt.minusMinutes(rules.windowMinutes());
        long count = depositRepository.countByActorAndUploadedAtAfter(doc.uploadedBy(), windowStart);
        if (SiemRules.exceedsFrequency(count, rules.maxDeposits())) {
            raise(doc, "FREQUENCE_ANORMALE", "MOYENNE",
                    count + " depots par '" + doc.uploadedBy() + "' en " + rules.windowMinutes() + " min");
        }
    }

    private void checkBusinessHours(DocumentPayload doc, OffsetDateTime uploadedAt) {
        int hour = uploadedAt.getHour();
        if (SiemRules.isOutsideBusinessHours(hour, rules.businessHoursStart(), rules.businessHoursEnd())) {
            raise(doc, "HORAIRE_INHABITUEL", "FAIBLE", "depot a " + hour + "h, hors plage habituelle");
        }
    }

    private void checkExtension(DocumentPayload doc) {
        if (SiemRules.isSuspiciousExtension(doc.filename(), suspiciousExtensions)) {
            raise(doc, "EXTENSION_SUSPECTE", "ELEVEE", "fichier '" + doc.filename() + "' d'extension suspecte");
        }
    }

    private void raise(DocumentPayload doc, String rule, String severity, String detail) {
        alertRepository.save(new Alert(doc.id(), rule, severity, detail, OffsetDateTime.now()));
        log.info("alerte siem : doc_id={} rule={} severity={} detail={}", doc.id(), rule, severity, detail);
    }

    private OffsetDateTime parseUploadedAt(String raw) {
        try {
            return OffsetDateTime.parse(raw);
        } catch (Exception e) {
            return OffsetDateTime.now();
        }
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
