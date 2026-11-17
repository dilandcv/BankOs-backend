package com.bankos.modules.tenants.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String code,
        String name,
        String status,
        String primaryColor,
        String secondaryColor,
        String logoUrl,
        Instant createdAt
) {
}
