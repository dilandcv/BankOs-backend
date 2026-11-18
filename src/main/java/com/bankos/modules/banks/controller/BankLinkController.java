package com.bankos.modules.banks.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.banks.dto.AvailableBankResponse;
import com.bankos.modules.banks.dto.ExchangeTokenRequest;
import com.bankos.modules.banks.dto.ExchangeTokenResponse;
import com.bankos.modules.banks.dto.LinkBankRequest;
import com.bankos.modules.banks.dto.LinkedBankResponse;
import com.bankos.modules.banks.service.BankLinkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BankLinkController {

    private final BankLinkService bankLinkService;

    public BankLinkController(BankLinkService bankLinkService) {
        this.bankLinkService = bankLinkService;
    }

    @GetMapping("/banks/available")
    public ApiResponse<List<AvailableBankResponse>> getAvailableBanks() {
        UUID userId = TenantContext.requireUserId();
        return ApiResponse.success(
                "AVAILABLE_BANKS_LISTED",
                "Instituciones bancarias disponibles",
                bankLinkService.getAvailableBanks(userId)
        );
    }

    @PostMapping("/banks/link")
    public ApiResponse<LinkedBankResponse> linkBank(@Valid @RequestBody LinkBankRequest request) {
        UUID userId = TenantContext.requireUserId();
        return ApiResponse.success(
                "BANK_LINKED",
                "Cuenta bancaria vinculada correctamente",
                bankLinkService.linkBank(userId, request)
        );
    }

    @GetMapping("/user/banks")
    public ApiResponse<List<LinkedBankResponse>> getLinkedBanks() {
        UUID userId = TenantContext.requireUserId();
        return ApiResponse.success(
                "LINKED_BANKS_LISTED",
                "Cuentas bancarias vinculadas",
                bankLinkService.getLinkedBanks(userId)
        );
    }

    @PatchMapping("/user/banks/{linkId}/default")
    public ApiResponse<Void> setDefaultBank(@PathVariable UUID linkId) {
        UUID userId = TenantContext.requireUserId();
        bankLinkService.setDefault(userId, linkId);
        return ApiResponse.success(
                "DEFAULT_BANK_UPDATED",
                "Cuenta bancaria predeterminada actualizada",
                null
        );
    }

    @DeleteMapping("/user/banks/{linkId}")
    public ApiResponse<Void> unlinkBank(@PathVariable UUID linkId) {
        UUID userId = TenantContext.requireUserId();
        bankLinkService.unlinkBank(userId, linkId);
        return ApiResponse.success(
                "BANK_UNLINKED",
                "Cuenta bancaria desvinculada correctamente",
                null
        );
    }

    @PostMapping("/user/banks/{linkId}/reauthorize")
    public ApiResponse<LinkedBankResponse> reauthorizeBank(@PathVariable UUID linkId) {
        UUID userId = TenantContext.requireUserId();
        return ApiResponse.success(
                "BANK_REAUTHORIZED",
                "Cuenta bancaria reautorizada correctamente",
                bankLinkService.reauthorize(userId, linkId)
        );
    }

    @PostMapping("/auth/exchange-bank-token")
    public ApiResponse<ExchangeTokenResponse> exchangeBankToken(@Valid @RequestBody ExchangeTokenRequest request) {
        UUID userId = TenantContext.requireUserId();
        return ApiResponse.success(
                "TOKEN_EXCHANGED",
                "Acceso concedido a la institución bancaria",
                bankLinkService.exchangeBankToken(userId, request.tenantCode())
        );
    }
}
