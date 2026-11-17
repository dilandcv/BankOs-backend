package com.bankos.modules.webhooks.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.common.response.PageResponse;
import com.bankos.modules.webhooks.dto.CreateWebhookRequest;
import com.bankos.modules.webhooks.dto.OutboxEventResponse;
import com.bankos.modules.webhooks.dto.WebhookResponse;
import com.bankos.modules.webhooks.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'AUDITOR')")
    public ApiResponse<List<WebhookResponse>> list() {
        return ApiResponse.success(
                "WEBHOOKS_LISTED",
                "Webhooks de la institución",
                webhookService.listConfigs()
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<WebhookResponse> create(@Valid @RequestBody CreateWebhookRequest request) {
        return ApiResponse.success(
                "WEBHOOK_CREATED",
                "Webhook creado correctamente",
                webhookService.createConfig(request)
        );
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'AUDITOR')")
    public ApiResponse<PageResponse<OutboxEventResponse>> events(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(
                "OUTBOX_EVENTS_LISTED",
                "Eventos de webhook",
                webhookService.listEvents(pageable)
        );
    }
}
