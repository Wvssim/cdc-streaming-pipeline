package ma.wvssim.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enveloppe d'un evenement CDC Debezium (le contenu du bloc {@code payload}).
 *
 * @param <T> le type des lignes portees par before/after (ici {@link DocumentPayload})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebeziumEnvelope<T>(
        T before,
        T after,
        // op : c = create, u = update, d = delete, r = read (snapshot)
        String op,
        @JsonProperty("ts_ms") Long tsMs,
        Source source
) {

    public boolean isCreate() {
        return "c".equals(op);
    }

    public boolean isDelete() {
        return "d".equals(op);
    }

    /** Metadonnees de la source (extrait des champs utiles du bloc {@code source}). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(
            String db,
            String schema,
            String table,
            Long lsn,
            @JsonProperty("ts_ms") Long tsMs
    ) {
    }
}
