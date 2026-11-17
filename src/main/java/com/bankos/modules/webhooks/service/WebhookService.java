package com.bankos.modules.webhooks.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.response.PageResponse;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.webhooks.dto.CreateWebhookRequest;
import com.bankos.modules.webhooks.dto.OutboxEventResponse;
import com.bankos.modules.webhooks.dto.WebhookResponse;
import com.bankos.modules.webhooks.entity.OutboxEvent;
import com.bankos.modules.webhooks.entity.OutboxStatus;
import com.bankos.modules.webhooks.entity.WebhookConfig;
import com.bankos.modules.webhooks.entity.WebhookStatus;
import com.bankos.modules.webhooks.mapper.WebhookMapper;
import com.bankos.modules.webhooks.repository.OutboxEventRepository;
import com.bankos.modules.webhooks.repository.WebhookConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookService {

    private final WebhookConfigRepository webhookConfigRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public WebhookService(
            WebhookConfigRepository webhookConfigRepository,
            OutboxEventRepository outboxEventRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.webhookConfigRepository = webhookConfigRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(UUID tenantId, String eventType, Object payload) {
        List<WebhookConfig> configs = webhookConfigRepository
                .findAllByTenantIdAndStatus(tenantId, WebhookStatus.ACTIVE);
        if (configs.isEmpty()) {
            return;
        }

        String payloadJson = toJson(payload);
        for (WebhookConfig config : configs) {
            if (!supports(config.getEvents(), eventType)) {
                continue;
            }
            OutboxEvent event = new OutboxEvent();
            event.setTenantId(tenantId);
            event.setWebhookConfigId(config.getId());
            event.setEventType(eventType);
            event.setPayload(payloadJson);
            event.setStatus(OutboxStatus.PENDING);
            event.setAttempts(0);
            event.setNextRetryAt(Instant.now());
            outboxEventRepository.save(event);
        }
    }

    @Transactional(readOnly = true)
    public List<WebhookResponse> listConfigs() {
        UUID tenantId = TenantContext.requireTenantId();
        return webhookConfigRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(WebhookMapper::toResponse)
                .toList();
    }

    @Transactional
    public WebhookResponse createConfig(CreateWebhookRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorUserId = TenantContext.requireUserId();
        String url = request.url().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "La URL del webhook debe iniciar con http:// o https://");
        }

        WebhookConfig config = new WebhookConfig();
        config.setTenantId(tenantId);
        config.setUrl(url);
        config.setEvents(request.events().trim());
        config.setSecret(request.secret());
        config.setStatus(WebhookStatus.ACTIVE);

        WebhookConfig saved = webhookConfigRepository.save(config);
        auditService.record(
                tenantId,
                actorUserId,
                "WEBHOOK_CREATED",
                "WEBHOOK_CONFIG",
                saved.getId(),
                Map.of("url", saved.getUrl(), "events", saved.getEvents())
        );
        return WebhookMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<OutboxEventResponse> listEvents(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return PageResponse.from(outboxEventRepository.findAllByTenantId(tenantId, pageable)
                .map(WebhookMapper::toResponse));
    }

    private boolean supports(String configuredEvents, String eventType) {
        return Arrays.stream(configuredEvents.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .anyMatch(value -> value.equals("*") || value.equals(eventType));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "No se pudo serializar el evento de webhook");
        }
    }
}
