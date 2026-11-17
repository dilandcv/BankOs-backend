package com.bankos.modules.transactions.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.common.response.PageResponse;
import com.bankos.modules.transactions.dto.DepositRequest;
import com.bankos.modules.transactions.dto.TransactionResponse;
import com.bankos.modules.transactions.dto.TransferPreviewRequest;
import com.bankos.modules.transactions.dto.TransferPreviewResponse;
import com.bankos.modules.transactions.dto.TransferRequest;
import com.bankos.modules.transactions.dto.WithdrawalRequest;
import com.bankos.modules.transactions.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposits")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'OPERATOR')")
    public ApiResponse<TransactionResponse> deposit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request
    ) {
        return transactionService.deposit(idempotencyKey, request);
    }

    @PostMapping("/withdrawals")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'OPERATOR')")
    public ApiResponse<TransactionResponse> withdrawal(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request
    ) {
        return transactionService.withdrawal(idempotencyKey, request);
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'OPERATOR')")
    public ApiResponse<TransactionResponse> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        return transactionService.transfer(idempotencyKey, request);
    }

    @PostMapping("/transfers/preview")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<TransferPreviewResponse> preview(
            @Valid @RequestBody TransferPreviewRequest request
    ) {
        return ApiResponse.success(
                "TRANSFER_PREVIEW",
                "Vista previa de transferencia",
                transactionService.preview(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<PageResponse<TransactionResponse>> history(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(
                "TRANSACTIONS_LISTED",
                "Historial de transacciones",
                transactionService.history(accountId, type, from, to, pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<TransactionResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(
                "TRANSACTION_FOUND",
                "Transacción encontrada",
                transactionService.get(id)
        );
    }
}
