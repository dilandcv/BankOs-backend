package com.bankos.modules.cards.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.modules.cards.dto.CardResponse;
import com.bankos.modules.cards.dto.CreateCardRequest;
import com.bankos.modules.cards.dto.UpdateCardStatusRequest;
import com.bankos.modules.cards.service.CardService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<List<CardResponse>> list(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID customerId
    ) {
        return ApiResponse.success(
                "CARDS_LISTED",
                "Tarjetas obtenidas correctamente",
                cardService.listCards(accountId, customerId)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR')")
    public ApiResponse<CardResponse> create(@Valid @RequestBody CreateCardRequest request) {
        return ApiResponse.success(
                "CARD_CREATED",
                "Tarjeta emitida correctamente",
                cardService.createCard(request)
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR')")
    public ApiResponse<CardResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCardStatusRequest request
    ) {
        return ApiResponse.success(
                "CARD_STATUS_UPDATED",
                "Estado de la tarjeta actualizado correctamente",
                cardService.updateStatus(id, request)
        );
    }
}
