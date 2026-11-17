package com.bankos.modules.transactions.repository;

import com.bankos.modules.transactions.entity.TransactionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, UUID> {

    List<TransactionEntry> findAllByTransactionIdAndTenantIdOrderByCreatedAtAsc(
            UUID transactionId,
            UUID tenantId
    );
}
