package com.bankos.modules.webhooks.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookResponse(
        UUID id,
        String url,
        String events,
        String status,
        Instant createdAt
) {
}
