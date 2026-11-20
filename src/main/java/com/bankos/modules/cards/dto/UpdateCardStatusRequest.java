package com.bankos.modules.cards.dto;

import com.bankos.modules.cards.entity.CardStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCardStatusRequest(
        @NotNull(message = "El estado de la tarjeta es obligatorio")
        CardStatus status
) {
}
