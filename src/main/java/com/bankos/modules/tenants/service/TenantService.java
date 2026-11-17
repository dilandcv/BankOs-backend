package com.bankos.modules.tenants.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.tenants.dto.CreateTenantRequest;
import com.bankos.modules.tenants.dto.TenantResponse;
import com.bankos.modules.tenants.entity.Tenant;
import com.bankos.modules.tenants.entity.TenantStatus;
import com.bankos.modules.tenants.mapper.TenantMapper;
import com.bankos.modules.tenants.repository.TenantRepository;
import com.bankos.modules.users.entity.Role;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public TenantService(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (tenantRepository.existsByCode(code)) {
            throw new ApiException(ErrorCode.CONFLICT, "Ya existe una institución con el código " + code);
        }

        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(request.name().trim());
        tenant.setPrimaryColor(request.primaryColor());
        tenant.setSecondaryColor(request.secondaryColor());
        tenant.setLogoUrl(request.logoUrl());
        tenant.setStatus(TenantStatus.ACTIVE);

        Tenant saved = tenantRepository.save(tenant);
        auditService.record(
                saved.getId(),
                TenantContext.requireUserId(),
                "TENANT_CREATED",
                "TENANT",
                saved.getId(),
                Map.of("code", saved.getCode(), "name", saved.getName())
        );
        return TenantMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID id) {
        Tenant tenant = requireAccessibleTenant(id);
        return TenantMapper.toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        if (TenantContext.hasRole(Role.PLATFORM_ADMIN)) {
            return tenantRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                    .stream()
                    .map(TenantMapper::toResponse)
                    .toList();
        }
        UUID tenantId = TenantContext.requireTenantId();
        return List.of(TenantMapper.toResponse(requireTenant(tenantId)));
    }

    @Transactional(readOnly = true)
    public Tenant requireTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TENANT_NOT_FOUND));
    }

    private Tenant requireAccessibleTenant(UUID id) {
        if (!TenantContext.hasRole(Role.PLATFORM_ADMIN)
                && !TenantContext.requireTenantId().equals(id)) {
            throw new ApiException(ErrorCode.TENANT_ACCESS_DENIED);
        }
        return requireTenant(id);
    }
}
