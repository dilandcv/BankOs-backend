package com.bankos.modules.cards.mapper;

import com.bankos.modules.cards.dto.CardResponse;
import com.bankos.modules.cards.entity.Card;

public final class CardMapper {

    private CardMapper() {
    }

    public static CardResponse toResponse(Card card) {
        return new CardResponse(
                card.getId(),
                card.getTenantId(),
                card.getAccountId(),
                card.getCustomerId(),
                card.getCardNumber(),
                card.getCardholderName(),
                card.getExpirationDate(),
                card.getCvvMasked(),
                card.getCardType(),
                card.getStatus(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
