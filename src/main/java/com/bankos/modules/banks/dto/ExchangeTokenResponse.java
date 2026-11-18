package com.bankos.modules.banks.dto;

import com.bankos.modules.tenants.dto.TenantSummary;
import com.bankos.modules.users.dto.UserSummary;

public record ExchangeTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user,
        TenantSummary tenant
) {
}
