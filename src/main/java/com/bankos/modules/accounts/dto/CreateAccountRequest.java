package com.bankos.modules.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateAccountRequest(
        @NotNull UUID customerId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "La moneda debe tener 3 letras")
        String currency
) {
}
