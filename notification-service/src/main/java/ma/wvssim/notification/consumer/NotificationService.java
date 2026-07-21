package ma.wvssim.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.wvssim.common.DebeziumEnvelope;
import ma.wvssim.common.DebeziumMessage;
import ma.wvssim.common.DocumentPayload;
import ma.wvssim.notification.domain.Notification;
import ma.wvssim.notification.domain.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/** Transforme un evenement CDC de creation de document en e-mail (MailHog en dev). */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final TypeReference<DebeziumMessage<DocumentPayload>> EVENT_TYPE = new TypeReference<>() {
    };

    private final NotificationRepository repository;
    private final JavaMailSender mailSender;
    private final String recipientDomain;
    // Cree ici : les records de common se deserialisent nativement, aucune config requise.
    private final ObjectMapper mapper = new ObjectMapper();

    public NotificationService(NotificationRepository repository, JavaMailSender mailSender,
                                @Value("${notification.recipient-domain}") String recipientDomain) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.recipientDomain = recipientDomain;
    }

    /** Idempotent : une re-livraison (at-least-once) ne renvoie pas un second e-mail. */
    @Transactional
    public void notify(String rawEvent) {
        DebeziumEnvelope<DocumentPayload> envelope = parse(rawEvent);
        // Seule la creation declenche une notification (pas de mise a jour/suppression pour l'instant).
        if (envelope == null || !envelope.isCreate() || envelope.after() == null) {
            return;
        }

        DocumentPayload doc = envelope.after();
        if (doc.id() == null || repository.existsByDocId(doc.id())) {
            return; // deja notifie
        }

        String recipient = doc.uploadedBy() + "@" + recipientDomain;
        String status = send(recipient, doc);

        repository.save(new Notification(doc.id(), recipient, status, OffsetDateTime.now()));
        log.info("notification : doc_id={} recipient={} status={}", doc.id(), recipient, status);
    }

    private String send(String recipient, DocumentPayload doc) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject("Nouveau document depose : " + doc.filename());
            message.setText("Le document '" + doc.filename() + "' (id " + doc.id() + ") a ete depose avec succes.");
            mailSender.send(message);
            return "ENVOYE";
        } catch (Exception e) {
            log.warn("envoi du mail impossible pour doc_id={} : {}", doc.id(), e.getMessage());
            return "ECHEC";
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
