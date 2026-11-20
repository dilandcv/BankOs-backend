package com.bankos.modules.tenants.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.tenants.dto.CreateTenantRequest;
import com.bankos.modules.tenants.dto.TenantResponse;
import com.bankos.modules.tenants.dto.TenantSummary;
import com.bankos.modules.tenants.dto.UpdateBrandingRequest;
import com.bankos.modules.tenants.entity.Tenant;
import com.bankos.modules.tenants.entity.TenantStatus;
import com.bankos.modules.tenants.mapper.TenantMapper;
import com.bankos.modules.tenants.repository.TenantRepository;
import com.bankos.modules.users.entity.Role;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    public List<TenantSummary> listPublic() {
        return tenantRepository.findAllByStatusOrderByNameAsc(TenantStatus.ACTIVE)
                .stream()
                .map(TenantMapper::toSummary)
                .toList();
    }

    @Transactional
    public TenantResponse updateBranding(UUID id, UpdateBrandingRequest request) {
        Tenant tenant = requireAccessibleTenant(id);

        if (request.primaryColor() != null && !request.primaryColor().isBlank()) {
            tenant.setPrimaryColor(request.primaryColor().trim());
        }
        if (request.secondaryColor() != null && !request.secondaryColor().isBlank()) {
            tenant.setSecondaryColor(request.secondaryColor().trim());
        }
        if (request.logoUrl() != null) {
            tenant.setLogoUrl(request.logoUrl().trim().isEmpty() ? null : request.logoUrl().trim());
        }

        Tenant saved = tenantRepository.save(tenant);
        UUID actorUserId = TenantContext.get() != null ? TenantContext.get().userId() : null;
        auditService.record(
                saved.getId(),
                actorUserId,
                "TENANT_BRANDING_UPDATED",
                "TENANT",
                saved.getId(),
                Map.of(
                        "primary_color", saved.getPrimaryColor() != null ? saved.getPrimaryColor() : "",
                        "secondary_color", saved.getSecondaryColor() != null ? saved.getSecondaryColor() : "",
                        "logo_url", saved.getLogoUrl() != null ? saved.getLogoUrl() : ""
                )
        );
        return TenantMapper.toResponse(saved);
    }

    @Transactional
    public TenantResponse uploadLogo(UUID id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "El archivo de imagen es obligatorio");
        }

        Tenant tenant = requireAccessibleTenant(id);

        String originalFilename = file.getOriginalFilename();
        String extension = "png";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!List.of("png", "jpg", "jpeg", "svg", "webp").contains(extension)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Formato de imagen no soportado. Formatos válidos: PNG, JPG, JPEG, SVG, WEBP");
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path uploadDir = Paths.get("uploads", "logos");
        try {
            Files.createDirectories(uploadDir);
            Path destination = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "No se pudo guardar la imagen del logo");
        }

        String logoUrl = "/uploads/logos/" + filename;
        tenant.setLogoUrl(logoUrl);
        Tenant saved = tenantRepository.save(tenant);

        UUID actorUserId = TenantContext.get() != null ? TenantContext.get().userId() : null;
        auditService.record(
                saved.getId(),
                actorUserId,
                "TENANT_LOGO_UPLOADED",
                "TENANT",
                saved.getId(),
                Map.of("logo_url", logoUrl)
        );

        return TenantMapper.toResponse(saved);
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
