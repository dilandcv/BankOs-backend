package com.bankos.modules.audit.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.response.PageResponse;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.common.web.CorrelationIdContext;
import com.bankos.modules.audit.dto.AuditLogResponse;
import com.bankos.modules.audit.entity.AuditLog;
import com.bankos.modules.audit.mapper.AuditMapper;
import com.bankos.modules.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(
            UUID tenantId,
            UUID actorUserId,
            String action,
            String resourceType,
            UUID resourceId,
            Object metadata
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(tenantId);
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setCorrelationId(CorrelationIdContext.get());
        auditLog.setMetadata(toJson(metadata));
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return PageResponse.from(auditLogRepository.findAllByTenantId(tenantId, pageable)
                .map(AuditMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public AuditLogResponse get(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        AuditLog auditLog = auditLogRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUDIT_LOG_NOT_FOUND));
        return AuditMapper.toResponse(auditLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "No se pudo serializar la auditoría");
        }
    }
}
