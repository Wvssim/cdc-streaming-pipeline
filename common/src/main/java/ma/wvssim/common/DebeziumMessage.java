package ma.wvssim.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Message Kafka brut produit par Debezium lorsque {@code value.converter.schemas.enable=true} :
 * un bloc {@code schema} (ignore ici) et un bloc {@code payload} qui porte l'enveloppe CDC.
 *
 * @param <T> le type des lignes portees par l'enveloppe (ici {@link DocumentPayload})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebeziumMessage<T>(DebeziumEnvelope<T> payload) {
}
