package ma.wvssim.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Une ligne de la table {@code public.documents}, telle qu'elle apparait dans les
 * blocs {@code before} / {@code after} de l'enveloppe Debezium.
 *
 * <p>{@code uploaded_at} reste une chaine (format ZonedTimestamp de Debezium) : le
 * parsing en date est laisse au service qui en a besoin.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentPayload(
        Long id,
        String filename,
        @JsonProperty("content_type") String contentType,
        Long size,
        @JsonProperty("storage_key") String storageKey,
        @JsonProperty("uploaded_by") String uploadedBy,
        @JsonProperty("uploaded_at") String uploadedAt
) {
}
