package com.bankos.modules.audit.mapper;

import com.bankos.modules.audit.dto.AuditLogResponse;
import com.bankos.modules.audit.entity.AuditLog;

public final class AuditMapper {

    private AuditMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getMetadata(),
                auditLog.getCorrelationId(),
                auditLog.getCreatedAt()
        );
    }
}
