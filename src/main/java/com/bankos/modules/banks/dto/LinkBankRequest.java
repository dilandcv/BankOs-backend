package com.bankos.modules.banks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkBankRequest(
        @NotBlank @Size(max = 50) String tenantCode,
        @Size(max = 100) String accountNickname,
        @Size(max = 50) String accountType,
        String consentToken
) {
}
