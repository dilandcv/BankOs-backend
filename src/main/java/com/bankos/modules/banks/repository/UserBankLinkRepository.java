package com.bankos.modules.banks.repository;

import com.bankos.modules.banks.entity.UserBankLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBankLinkRepository extends JpaRepository<UserBankLink, UUID> {

    List<UserBankLink> findAllByUserId(UUID userId);

    Optional<UserBankLink> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<UserBankLink> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
}
