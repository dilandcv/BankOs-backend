package com.bankos.modules.cards.repository;

import com.bankos.modules.cards.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findAllByTenantId(UUID tenantId);

    List<Card> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Card> findAllByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<Card> findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    List<Card> findAllByTenantIdAndAccountId(UUID tenantId, UUID accountId);

    Optional<Card> findByIdAndTenantId(UUID id, UUID tenantId);
}
