package com.bankos.modules.users.dto;

import com.bankos.modules.users.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull Role role
) {
}
