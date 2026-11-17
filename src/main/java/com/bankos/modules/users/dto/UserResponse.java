package com.bankos.modules.users.dto;

import com.bankos.modules.users.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        String status,
        Instant createdAt
) {
}
