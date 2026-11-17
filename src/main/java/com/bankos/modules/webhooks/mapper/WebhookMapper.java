package com.bankos.modules.webhooks.mapper;

import com.bankos.modules.webhooks.dto.OutboxEventResponse;
import com.bankos.modules.webhooks.dto.WebhookResponse;
import com.bankos.modules.webhooks.entity.OutboxEvent;
import com.bankos.modules.webhooks.entity.WebhookConfig;

public final class WebhookMapper {

    private WebhookMapper() {
    }

    public static WebhookResponse toResponse(WebhookConfig config) {
        return new WebhookResponse(
                config.getId(),
                config.getUrl(),
                config.getEvents(),
                config.getStatus().name(),
                config.getCreatedAt()
        );
    }

    public static OutboxEventResponse toResponse(OutboxEvent event) {
        return new OutboxEventResponse(
                event.getId(),
                event.getWebhookConfigId(),
                event.getEventType(),
                event.getPayload(),
                event.getStatus().name(),
                event.getAttempts(),
                event.getNextRetryAt(),
                event.getProcessedAt(),
                event.getCreatedAt()
        );
    }
}
