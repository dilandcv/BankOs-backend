package com.bankos.modules.settings.mapper;

import com.bankos.modules.settings.dto.ExchangeRateResponse;
import com.bankos.modules.settings.dto.TenantSettingsResponse;
import com.bankos.modules.settings.entity.ExchangeRate;
import com.bankos.modules.settings.entity.TenantSetting;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class SettingsMapper {

    private SettingsMapper() {
    }

    public static TenantSettingsResponse toSettingsResponse(List<TenantSetting> settings) {
        Map<String, String> values = new TreeMap<>();
        for (TenantSetting setting : settings) {
            values.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return new TenantSettingsResponse(values);
    }

    public static ExchangeRateResponse toResponse(ExchangeRate exchangeRate) {
        return new ExchangeRateResponse(
                exchangeRate.getId(),
                exchangeRate.getFromCurrency(),
                exchangeRate.getToCurrency(),
                exchangeRate.getRate(),
                exchangeRate.getEffectiveFrom(),
                exchangeRate.getEffectiveTo(),
                exchangeRate.isActive(),
                exchangeRate.getCreatedAt()
        );
    }
}
