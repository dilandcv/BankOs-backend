package com.bankos.modules.banks.dto;

import java.time.Instant;
import java.util.UUID;

public record LinkedBankResponse(
        UUID linkId,
        UUID tenantId,
        String tenantCode,
        String tenantName,
        String primaryColor,
        String secondaryColor,
        String logoUrl,
        String accountNickname,
        String accountNumberMask,
        String accountType,
        String authStatus,
        String authMechanism,
        boolean isDefault,
        Instant lastSyncAt
) {
}
