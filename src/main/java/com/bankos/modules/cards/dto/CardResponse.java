package com.bankos.modules.cards.dto;

import com.bankos.modules.cards.entity.CardStatus;
import com.bankos.modules.cards.entity.CardType;

import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID tenantId,
        UUID accountId,
        UUID customerId,
        String cardNumber,
        String cardholderName,
        String expirationDate,
        String cvvMasked,
        CardType cardType,
        CardStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
