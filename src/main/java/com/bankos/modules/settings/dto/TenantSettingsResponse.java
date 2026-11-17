package com.bankos.modules.settings.dto;

import java.util.Map;

public record TenantSettingsResponse(
        Map<String, String> settings
) {
}
