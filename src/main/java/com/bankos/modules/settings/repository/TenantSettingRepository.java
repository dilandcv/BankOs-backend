package com.bankos.modules.settings.repository;

import com.bankos.modules.settings.entity.TenantSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantSettingRepository extends JpaRepository<TenantSetting, UUID> {

    Optional<TenantSetting> findByTenantIdAndSettingKey(UUID tenantId, String settingKey);

    List<TenantSetting> findAllByTenantIdOrderBySettingKeyAsc(UUID tenantId);
}
