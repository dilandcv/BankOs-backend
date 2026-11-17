package com.bankos.modules.accounts.repository;

import com.bankos.modules.accounts.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByIdAndTenantId(UUID id, UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id and a.tenantId = :tenantId")
    Optional<Account> findByIdAndTenantIdForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    Page<Account> findAllByTenantId(UUID tenantId, Pageable pageable);

    List<Account> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    boolean existsByTenantIdAndAccountNumber(UUID tenantId, String accountNumber);
}
