package com.bankos.modules.banks.dto;

import java.util.UUID;

public record AvailableBankResponse(
        UUID tenantId,
        String code,
        String name,
        String primaryColor,
        String secondaryColor,
        String logoUrl,
        String defaultAuthMechanism,
        boolean isLinked
) {
}
