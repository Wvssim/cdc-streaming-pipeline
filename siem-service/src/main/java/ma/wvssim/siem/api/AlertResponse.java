package ma.wvssim.siem.api;

import ma.wvssim.siem.domain.Alert;

import java.time.OffsetDateTime;

public record AlertResponse(
        Long id,
        Long docId,
        String rule,
        String severity,
        String detail,
        OffsetDateTime raisedAt
) {

    public static AlertResponse from(Alert a) {
        return new AlertResponse(a.getId(), a.getDocId(), a.getRule(), a.getSeverity(), a.getDetail(), a.getRaisedAt());
    }
}
