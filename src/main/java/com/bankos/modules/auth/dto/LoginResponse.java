package com.bankos.modules.auth.dto;

import com.bankos.modules.banks.dto.LinkedBankResponse;
import com.bankos.modules.banks.dto.MasterUserSummary;
import com.bankos.modules.tenants.dto.TenantSummary;
import com.bankos.modules.users.dto.UserSummary;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user,
        TenantSummary tenant,
        MasterUserSummary masterUser,
        List<LinkedBankResponse> linkedBanks
) {
    public LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            UserSummary user,
            TenantSummary tenant
    ) {
        this(accessToken, tokenType, expiresIn, user, tenant, null, null);
    }
}
