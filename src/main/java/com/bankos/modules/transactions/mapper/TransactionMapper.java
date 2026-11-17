package com.bankos.modules.transactions.mapper;

import com.bankos.modules.transactions.dto.TransactionEntryResponse;
import com.bankos.modules.transactions.dto.TransactionResponse;
import com.bankos.modules.transactions.entity.Transaction;
import com.bankos.modules.transactions.entity.TransactionEntry;

import java.util.List;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionResponse toResponse(Transaction transaction, List<TransactionEntry> entries) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getFee(),
                transaction.getExchangeRate(),
                transaction.getSourceAccountId(),
                transaction.getDestinationAccountId(),
                transaction.getDescription(),
                transaction.getCorrelationId(),
                transaction.getCreatedAt(),
                entries.stream().map(TransactionMapper::toEntryResponse).toList()
        );
    }

    public static TransactionEntryResponse toEntryResponse(TransactionEntry entry) {
        return new TransactionEntryResponse(
                entry.getId(),
                entry.getAccountId(),
                entry.getDirection().name(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getCreatedAt()
        );
    }
}
