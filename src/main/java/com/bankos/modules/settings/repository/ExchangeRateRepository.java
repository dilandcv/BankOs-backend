package com.bankos.modules.settings.repository;

import com.bankos.modules.settings.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    Optional<ExchangeRate> findFirstByTenantIdAndFromCurrencyAndToCurrencyAndActiveTrueOrderByEffectiveFromDesc(
            UUID tenantId,
            String fromCurrency,
            String toCurrency
    );

    List<ExchangeRate> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<ExchangeRate> findByIdAndTenantId(UUID id, UUID tenantId);
}
