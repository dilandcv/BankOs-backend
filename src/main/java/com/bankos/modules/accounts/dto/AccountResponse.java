package com.bankos.modules.accounts.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String accountNumber,
        String currency,
        BigDecimal balance,
        String status,
        Instant createdAt
) {
}
