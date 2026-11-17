package com.bankos.modules.settings.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.modules.settings.dto.CreateExchangeRateRequest;
import com.bankos.modules.settings.dto.ExchangeRateResponse;
import com.bankos.modules.settings.dto.TenantSettingsResponse;
import com.bankos.modules.settings.dto.UpdateSettingsRequest;
import com.bankos.modules.settings.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<TenantSettingsResponse> getSettings() {
        return ApiResponse.success(
                "SETTINGS_FOUND",
                "Configuración de la institución",
                settingsService.getSettings()
        );
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'PLATFORM_ADMIN')")
    public ApiResponse<TenantSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateSettingsRequest request
    ) {
        return ApiResponse.success(
                "SETTINGS_UPDATED",
                "Configuración actualizada correctamente",
                settingsService.updateSettings(request)
        );
    }

    @GetMapping("/exchange-rates")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<List<ExchangeRateResponse>> listExchangeRates() {
        return ApiResponse.success(
                "EXCHANGE_RATES_LISTED",
                "Tasas de cambio de la institución",
                settingsService.listExchangeRates()
        );
    }

    @GetMapping("/exchange-rates/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<ExchangeRateResponse> getExchangeRate(@PathVariable UUID id) {
        return ApiResponse.success(
                "EXCHANGE_RATE_FOUND",
                "Tasa de cambio encontrada",
                settingsService.getExchangeRate(id)
        );
    }

    @PostMapping("/exchange-rates")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'PLATFORM_ADMIN')")
    public ApiResponse<ExchangeRateResponse> createExchangeRate(
            @Valid @RequestBody CreateExchangeRateRequest request
    ) {
        return ApiResponse.success(
                "EXCHANGE_RATE_CREATED",
                "Tasa de cambio creada correctamente",
                settingsService.createExchangeRate(request)
        );
    }
}
