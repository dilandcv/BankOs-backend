package com.bankos.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Size(max = 50) String tenantCode,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String password
) {
}
