package com.bankos.modules.webhooks.dto;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventResponse(
        UUID id,
        UUID webhookConfigId,
        String eventType,
        String payload,
        String status,
        int attempts,
        Instant nextRetryAt,
        Instant processedAt,
        Instant createdAt
) {
}
