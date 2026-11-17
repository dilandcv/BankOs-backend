package com.bankos.modules.webhooks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWebhookRequest(
        @NotBlank @Size(max = 500) String url,
        @NotBlank @Size(max = 500) String events,
        @NotBlank @Size(min = 16, max = 255) String secret
) {
}
