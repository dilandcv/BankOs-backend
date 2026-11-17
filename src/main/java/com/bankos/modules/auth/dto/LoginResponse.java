package com.bankos.modules.auth.dto;

import com.bankos.modules.tenants.dto.TenantSummary;
import com.bankos.modules.users.dto.UserSummary;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user,
        TenantSummary tenant
) {
}
