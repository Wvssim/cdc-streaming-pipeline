package ma.wvssim.notification.api;

import ma.wvssim.notification.domain.Notification;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        Long docId,
        String recipient,
        String status,
        OffsetDateTime sentAt
) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getDocId(), n.getRecipient(), n.getStatus(), n.getSentAt());
    }
}
