package com.bankos.modules.transactions.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEntryResponse(
        UUID id,
        UUID accountId,
        String direction,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {
}
