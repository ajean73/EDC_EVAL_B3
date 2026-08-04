package fr.edc3.pmt.api.dto;

import fr.edc3.pmt.domain.enums.NotificationKind;

import java.time.LocalDateTime;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            Long accountId,
            Long workItemId,
            NotificationKind kind,
            Boolean isSent,
            LocalDateTime sentAt,
            LocalDateTime createdAt
    ) {
    }
}
