package com.bankos.modules.settings.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.money.MoneyUtils;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.settings.dto.CreateExchangeRateRequest;
import com.bankos.modules.settings.dto.ExchangeRateResponse;
import com.bankos.modules.settings.dto.TenantSettingsResponse;
import com.bankos.modules.settings.dto.UpdateSettingsRequest;
import com.bankos.modules.settings.entity.ExchangeRate;
import com.bankos.modules.settings.entity.TenantSetting;
import com.bankos.modules.settings.mapper.SettingsMapper;
import com.bankos.modules.settings.repository.ExchangeRateRepository;
import com.bankos.modules.settings.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SettingsService {

    public static final String FEE_FIXED_KEY = "transfer.fee.fixed";
    public static final String FEE_PERCENT_KEY = "transfer.fee.percent";

    private final TenantSettingRepository tenantSettingRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final AuditService auditService;

    public SettingsService(
            TenantSettingRepository tenantSettingRepository,
            ExchangeRateRepository exchangeRateRepository,
            AuditService auditService
    ) {
        this.tenantSettingRepository = tenantSettingRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public TenantSettingsResponse getSettings() {
        UUID tenantId = TenantContext.requireTenantId();
        return SettingsMapper.toSettingsResponse(
                tenantSettingRepository.findAllByTenantIdOrderBySettingKeyAsc(tenantId)
        );
    }

    @Transactional
    public TenantSettingsResponse updateSettings(UpdateSettingsRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorUserId = TenantContext.requireUserId();

        for (Map.Entry<String, String> entry : request.settings().entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (key.isBlank() || key.length() > 100) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Cada clave de configuración debe tener entre 1 y 100 caracteres");
            }
            if (value.length() > 500) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "El valor de " + key + " supera 500 caracteres");
            }

            TenantSetting setting = tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, key)
                    .orElseGet(() -> {
                        TenantSetting created = new TenantSetting();
                        created.setTenantId(tenantId);
                        created.setSettingKey(key);
                        return created;
                    });
            setting.setSettingValue(value);
            tenantSettingRepository.save(setting);
        }

        auditService.record(
                tenantId,
                actorUserId,
                "SETTINGS_UPDATED",
                "TENANT_SETTINGS",
                tenantId,
                Map.of("keys", request.settings().keySet())
        );
        return getSettings();
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> listExchangeRates() {
        UUID tenantId = TenantContext.requireTenantId();
        return exchangeRateRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(SettingsMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExchangeRateResponse getExchangeRate(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        ExchangeRate exchangeRate = exchangeRateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT, "Tasa de cambio no encontrada"));
        return SettingsMapper.toResponse(exchangeRate);
    }

    @Transactional
    public ExchangeRateResponse createExchangeRate(CreateExchangeRateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorUserId = TenantContext.requireUserId();

        String fromCurrency;
        String toCurrency;
        try {
            fromCurrency = MoneyUtils.normalizeCurrency(request.fromCurrency());
            toCurrency = MoneyUtils.normalizeCurrency(request.toCurrency());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.CURRENCY_NOT_SUPPORTED);
        }

        if (fromCurrency.equals(toCurrency)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Las monedas de origen y destino deben ser diferentes");
        }
        if (request.effectiveTo() != null && !request.effectiveTo().isAfter(request.effectiveFrom())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "effective_to debe ser posterior a effective_from");
        }

        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setTenantId(tenantId);
        exchangeRate.setFromCurrency(fromCurrency);
        exchangeRate.setToCurrency(toCurrency);
        exchangeRate.setRate(MoneyUtils.rate(request.rate()));
        exchangeRate.setEffectiveFrom(request.effectiveFrom());
        exchangeRate.setEffectiveTo(request.effectiveTo());
        exchangeRate.setActive(true);

        ExchangeRate saved = exchangeRateRepository.save(exchangeRate);
        auditService.record(
                tenantId,
                actorUserId,
                "EXCHANGE_RATE_CREATED",
                "EXCHANGE_RATE",
                saved.getId(),
                Map.of(
                        "from_currency", saved.getFromCurrency(),
                        "to_currency", saved.getToCurrency(),
                        "rate", saved.getRate().toPlainString()
                )
        );
        return SettingsMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveRate(UUID tenantId, String fromCurrency, String toCurrency) {
        String from = MoneyUtils.normalizeCurrency(fromCurrency);
        String to = MoneyUtils.normalizeCurrency(toCurrency);
        if (from.equals(to)) {
            return BigDecimal.ONE.setScale(MoneyUtils.RATE_SCALE, RoundingMode.HALF_UP);
        }
        return exchangeRateRepository
                .findFirstByTenantIdAndFromCurrencyAndToCurrencyAndActiveTrueOrderByEffectiveFromDesc(
                        tenantId,
                        from,
                        to
                )
                .map(ExchangeRate::getRate)
                .map(MoneyUtils::rate)
                .orElseThrow(() -> new ApiException(ErrorCode.EXCHANGE_RATE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTransferFee(UUID tenantId, BigDecimal amount, String sourceCurrency) {
        BigDecimal fixed = getDecimalSetting(tenantId, FEE_FIXED_KEY, BigDecimal.ZERO);
        BigDecimal percent = getDecimalSetting(tenantId, FEE_PERCENT_KEY, BigDecimal.ZERO);

        BigDecimal percentageFee = amount
                .multiply(percent.max(BigDecimal.ZERO))
                .divide(BigDecimal.valueOf(100), MoneyUtils.MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = fixed.max(BigDecimal.ZERO).add(percentageFee);
        return MoneyUtils.money(total.max(BigDecimal.ZERO));
    }

    private BigDecimal getDecimalSetting(UUID tenantId, String key, BigDecimal defaultValue) {
        return tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, key)
                .map(TenantSetting::getSettingValue)
                .map(value -> {
                    try {
                        return new BigDecimal(value);
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
}
