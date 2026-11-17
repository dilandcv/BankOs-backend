package com.bankos.modules.customers.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String documentType,
        String documentNumber,
        String fullName,
        String status,
        Instant createdAt
) {
}
