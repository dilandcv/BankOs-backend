package com.bankos.modules.webhooks.repository;

import com.bankos.modules.webhooks.entity.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    Page<OutboxEvent> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<OutboxEvent> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query(value = """
            select * from outbox_events
            where status = 'PENDING'
              and next_retry_at <= now()
            order by created_at asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> lockPending(@Param("limit") int limit);
}
