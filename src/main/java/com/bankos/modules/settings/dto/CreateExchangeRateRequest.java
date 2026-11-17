package com.bankos.modules.settings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateExchangeRateRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String fromCurrency,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String toCurrency,
        @NotNull @DecimalMin("0.00000001") @Digits(integer = 11, fraction = 8) BigDecimal rate,
        @NotNull Instant effectiveFrom,
        Instant effectiveTo
) {
}
