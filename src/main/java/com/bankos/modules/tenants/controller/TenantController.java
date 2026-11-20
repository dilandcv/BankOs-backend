package com.bankos.modules.tenants.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.modules.tenants.dto.CreateTenantRequest;
import com.bankos.modules.tenants.dto.TenantResponse;
import com.bankos.modules.tenants.dto.TenantSummary;
import com.bankos.modules.tenants.dto.UpdateBrandingRequest;
import com.bankos.modules.tenants.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/public")
    public ApiResponse<List<TenantSummary>> listPublic() {
        return ApiResponse.success(
                "TENANTS_PUBLIC_LISTED",
                "Instituciones activas",
                tenantService.listPublic()
        );
    }

    @PatchMapping("/{id}/branding")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<TenantResponse> updateBranding(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBrandingRequest request
    ) {
        return ApiResponse.success(
                "TENANT_BRANDING_UPDATED",
                "Marca de la institución actualizada correctamente",
                tenantService.updateBranding(id, request)
        );
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ApiResponse<TenantResponse> uploadLogo(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(
                "TENANT_LOGO_UPLOADED",
                "Logo de la institución subido correctamente",
                tenantService.uploadLogo(id, file)
        );
    }
}
