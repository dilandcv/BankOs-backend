package com.bankos.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public final class MoneyUtils {

    public static final int MONEY_SCALE = 4;
    public static final int RATE_SCALE = 8;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private MoneyUtils() {
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING);
    }

    public static BigDecimal rate(BigDecimal value) {
        return value.setScale(RATE_SCALE, ROUNDING);
    }

    public static BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
    }

    public static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("La moneda es obligatoria");
        }
        String normalized = currency.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("La moneda debe tener 3 letras");
        }
        Currency.getInstance(normalized);
        return normalized;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
