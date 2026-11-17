package com.bankos.modules.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String action,
        String resourceType,
        UUID resourceId,
        String metadata,
        String correlationId,
        Instant createdAt
) {
}
