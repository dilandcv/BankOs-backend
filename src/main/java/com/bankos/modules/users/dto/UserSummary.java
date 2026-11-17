package com.bankos.modules.users.dto;

import com.bankos.modules.users.entity.Role;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        String fullName,
        Role role
) {
}
