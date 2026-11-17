package com.bankos.modules.idempotency.repository;

import com.bankos.modules.idempotency.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByTenantIdAndOperationAndIdempotencyKey(
            UUID tenantId,
            String operation,
            String idempotencyKey
    );
}
