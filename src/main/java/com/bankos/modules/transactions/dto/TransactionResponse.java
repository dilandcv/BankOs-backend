package com.bankos.modules.transactions.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        BigDecimal fee,
        BigDecimal exchangeRate,
        UUID sourceAccountId,
        UUID destinationAccountId,
        String description,
        String correlationId,
        Instant createdAt,
        List<TransactionEntryResponse> entries
) {
}
