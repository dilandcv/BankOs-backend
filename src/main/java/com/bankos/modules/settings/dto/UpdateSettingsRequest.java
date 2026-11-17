package com.bankos.modules.settings.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateSettingsRequest(
        @NotNull Map<String, String> settings
) {
}
