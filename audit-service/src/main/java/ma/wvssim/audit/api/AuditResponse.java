package ma.wvssim.audit.api;

import ma.wvssim.audit.domain.AuditLog;

import java.time.OffsetDateTime;

public record AuditResponse(
        String eventId,
        Long docId,
        String action,
        String actor,
        OffsetDateTime occurredAt
) {

    public static AuditResponse from(AuditLog a) {
        return new AuditResponse(a.getEventId(), a.getDocId(), a.getAction(), a.getActor(), a.getOccurredAt());
    }
}
