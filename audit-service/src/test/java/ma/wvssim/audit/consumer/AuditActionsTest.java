package ma.wvssim.audit.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditActionsTest {

    @Test
    void traduitLesOperationsDebezium() {
        assertEquals("CREATION", AuditActions.of("c"));
        assertEquals("MISE_A_JOUR", AuditActions.of("u"));
        assertEquals("SUPPRESSION", AuditActions.of("d"));
        assertEquals("SNAPSHOT", AuditActions.of("r"));
    }

    @Test
    void gereLesCasInconnus() {
        assertEquals("INCONNU", AuditActions.of(null));
        assertEquals("x", AuditActions.of("x"));
    }
}
