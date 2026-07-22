package ma.wvssim.siem.consumer;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiemRulesTest {

    @Test
    void detecteUneFrequenceAnormale() {
        assertTrue(SiemRules.exceedsFrequency(6, 5));
        assertFalse(SiemRules.exceedsFrequency(5, 5));
    }

    @Test
    void detecteUnHoraireInhabituel() {
        assertTrue(SiemRules.isOutsideBusinessHours(3, 7, 21));
        assertTrue(SiemRules.isOutsideBusinessHours(22, 7, 21));
        assertFalse(SiemRules.isOutsideBusinessHours(14, 7, 21));
    }

    @Test
    void detecteUneExtensionSuspecte() {
        Set<String> suspicious = Set.of("exe", "bat", "sh");
        assertTrue(SiemRules.isSuspiciousExtension("virus.exe", suspicious));
        assertTrue(SiemRules.isSuspiciousExtension("SCRIPT.SH", suspicious));
        assertFalse(SiemRules.isSuspiciousExtension("rapport.pdf", suspicious));
        assertFalse(SiemRules.isSuspiciousExtension("sansextension", suspicious));
    }
}
