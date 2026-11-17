package com.bankos.modules.tenants.dto;

import java.util.UUID;

public record TenantSummary(
        UUID id,
        String code,
        String name,
        String primaryColor,
        String secondaryColor,
        String logoUrl
) {
}
