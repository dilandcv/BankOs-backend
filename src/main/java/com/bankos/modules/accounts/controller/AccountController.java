package com.bankos.modules.accounts.controller;

import com.bankos.common.response.ApiResponse;
import com.bankos.common.response.PageResponse;
import com.bankos.modules.accounts.dto.AccountResponse;
import com.bankos.modules.accounts.dto.CreateAccountRequest;
import com.bankos.modules.accounts.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'OPERATOR')")
    public ApiResponse<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.success(
                "ACCOUNT_CREATED",
                "Cuenta creada correctamente",
                accountService.create(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<PageResponse<AccountResponse>> list(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(
                "ACCOUNTS_LISTED",
                "Cuentas de la institución",
                accountService.list(pageable)
        );
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<List<AccountResponse>> listAll() {
        return ApiResponse.success(
                "ACCOUNTS_LISTED",
                "Todas las cuentas de la institución",
                accountService.listAll()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'AUDITOR')")
    public ApiResponse<AccountResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(
                "ACCOUNT_FOUND",
                "Cuenta encontrada",
                accountService.get(id)
        );
    }
}
