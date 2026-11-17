package com.bankos.common.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyUtilsTest {

    @Test
    void roundsMoneyToFourDecimalsHalfUp() {
        assertThat(MoneyUtils.money(new BigDecimal("10.12345")))
                .isEqualByComparingTo("10.1235");
    }

    @Test
    void normalizesAndValidatesCurrency() {
        assertThat(MoneyUtils.normalizeCurrency("usd")).isEqualTo("USD");
        assertThatThrownBy(() -> MoneyUtils.normalizeCurrency("US"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void identifiesPositiveAmounts() {
        assertThat(MoneyUtils.isPositive(new BigDecimal("0.0001"))).isTrue();
        assertThat(MoneyUtils.isPositive(BigDecimal.ZERO)).isFalse();
        assertThat(MoneyUtils.isPositive(new BigDecimal("-1"))).isFalse();
    }
}
