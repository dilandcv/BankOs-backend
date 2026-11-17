package com.bankos.modules.webhooks.repository;

import com.bankos.modules.webhooks.entity.WebhookConfig;
import com.bankos.modules.webhooks.entity.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, UUID> {

    List<WebhookConfig> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<WebhookConfig> findByIdAndTenantId(UUID id, UUID tenantId);

    List<WebhookConfig> findAllByTenantIdAndStatus(UUID tenantId, WebhookStatus status);
}
