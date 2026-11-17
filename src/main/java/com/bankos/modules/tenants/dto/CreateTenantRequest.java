package com.bankos.modules.tenants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9_]+", message = "Solo letras, números y guion bajo")
        String code,
        @NotBlank @Size(max = 150) String name,
        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Debe ser un color hexadecimal #RRGGBB")
        String primaryColor,
        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Debe ser un color hexadecimal #RRGGBB")
        String secondaryColor,
        @Size(max = 500) String logoUrl
) {
}
