package com.bankos.modules.tenants.mapper;

import com.bankos.modules.tenants.dto.TenantResponse;
import com.bankos.modules.tenants.dto.TenantSummary;
import com.bankos.modules.tenants.entity.Tenant;

public final class TenantMapper {

    private TenantMapper() {
    }

    public static TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getStatus().name(),
                tenant.getPrimaryColor(),
                tenant.getSecondaryColor(),
                tenant.getLogoUrl(),
                tenant.getCreatedAt()
        );
    }

    public static TenantSummary toSummary(Tenant tenant) {
        return new TenantSummary(
                tenant.getId(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getPrimaryColor(),
                tenant.getSecondaryColor(),
                tenant.getLogoUrl()
        );
    }
}
