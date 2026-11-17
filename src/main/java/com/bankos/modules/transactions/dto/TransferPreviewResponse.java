package com.bankos.modules.transactions.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferPreviewResponse(
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal exchangeRate,
        BigDecimal fee,
        BigDecimal debitAmount,
        BigDecimal creditAmount
) {
}
