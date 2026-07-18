package ma.wvssim.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie que l'enveloppe se deserialise a partir de la vraie fixture capturee au jalon S2
 * ({@code docs/samples/document-created-event.json}), pas d'un echantillon invente.
 */
class DebeziumEnvelopeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Remonte depuis le repertoire courant jusqu'a trouver la fixture (robuste au cwd du build).
    private Path locateFixture() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 5 && dir != null; i++) {
            Path candidate = dir.resolve("docs/samples/document-created-event.json");
            if (Files.exists(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "fixture docs/samples/document-created-event.json introuvable depuis "
                        + System.getProperty("user.dir"));
    }

    @Test
    void parseLEvenementCreateReel() throws Exception {
        String json = Files.readString(locateFixture());

        DebeziumMessage<DocumentPayload> message =
                MAPPER.readValue(json, new TypeReference<DebeziumMessage<DocumentPayload>>() {
                });

        DebeziumEnvelope<DocumentPayload> envelope = message.payload();
        assertNotNull(envelope, "le bloc payload doit etre present");
        assertEquals("c", envelope.op());
        assertTrue(envelope.isCreate());
        assertNull(envelope.before(), "un INSERT n'a pas d'etat before");

        DocumentPayload after = envelope.after();
        assertNotNull(after, "le bloc after doit etre present");
        assertEquals(1L, after.id().longValue());
        assertEquals("test.pdf", after.filename());
        assertEquals("application/pdf", after.contentType());
        assertEquals(1024L, after.size().longValue());
        assertEquals("test-key", after.storageKey());
        assertEquals("wassim", after.uploadedBy());
        assertEquals("2026-07-13T23:13:38.259466Z", after.uploadedAt());

        DebeziumEnvelope.Source source = envelope.source();
        assertNotNull(source, "le bloc source doit etre present");
        assertEquals("docdb", source.db());
        assertEquals("public", source.schema());
        assertEquals("documents", source.table());
        assertEquals(26767256L, source.lsn().longValue());
    }
}
