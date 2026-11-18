package com.bankos.modules.banks.dto;

import java.util.UUID;

public record MasterUserSummary(
        UUID id,
        String email,
        String fullName,
        String phone
) {
}
