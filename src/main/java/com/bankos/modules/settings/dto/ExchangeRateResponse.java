package com.bankos.modules.settings.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExchangeRateResponse(
        UUID id,
        String fromCurrency,
        String toCurrency,
        BigDecimal rate,
        Instant effectiveFrom,
        Instant effectiveTo,
        boolean active,
        Instant createdAt
) {
}
