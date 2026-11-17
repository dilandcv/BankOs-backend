package com.bankos.modules.tenants.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.modules.tenants.dto.CreateTenantRequest;
import com.bankos.modules.tenants.dto.TenantResponse;
import com.bankos.modules.tenants.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        return ApiResponse.success(
                "TENANT_CREATED",
                "Institución creada correctamente",
                tenantService.create(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<List<TenantResponse>> list() {
        return ApiResponse.success(
                "TENANTS_LISTED",
                "Instituciones disponibles",
                tenantService.list()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<TenantResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(
                "TENANT_FOUND",
                "Institución encontrada",
                tenantService.get(id)
        );
    }
}
