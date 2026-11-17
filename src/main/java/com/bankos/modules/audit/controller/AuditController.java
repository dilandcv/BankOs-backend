package com.bankos.modules.audit.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.common.response.PageResponse;
import com.bankos.modules.audit.dto.AuditLogResponse;
import com.bankos.modules.audit.service.AuditService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'AUDITOR')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(
                "AUDIT_LOGS_LISTED",
                "Registros de auditoría",
                auditService.list(pageable)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<AuditLogResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(
                "AUDIT_LOG_FOUND",
                "Registro de auditoría encontrado",
                auditService.get(id)
        );
    }
}
