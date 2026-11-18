package com.bankos.modules.banks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExchangeTokenRequest(
        @NotBlank @Size(max = 50) String tenantCode
) {
}
