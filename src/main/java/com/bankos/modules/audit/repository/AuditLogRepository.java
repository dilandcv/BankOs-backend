package com.bankos.modules.audit.repository;

import com.bankos.modules.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<AuditLog> findByIdAndTenantId(UUID id, UUID tenantId);
}
