package com.bankos.modules.tenants.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBrandingRequest(
        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "El color primario debe ser un código HEX válido (#RRGGBB)")
        String primaryColor,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "El color secundario debe ser un código HEX válido (#RRGGBB)")
        String secondaryColor,

        @Size(max = 1000, message = "La URL del logo no puede exceder 1000 caracteres")
        String logoUrl
) {
}
