package com.bankos.modules.cards.dto;

import com.bankos.modules.cards.entity.CardType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCardRequest(
        @NotNull(message = "El ID de la cuenta es obligatorio")
        UUID accountId,
        UUID customerId,
        String cardholderName,
        CardType cardType
) {
}
