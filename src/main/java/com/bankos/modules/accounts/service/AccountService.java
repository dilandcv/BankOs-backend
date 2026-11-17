package com.bankos.modules.accounts.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.money.MoneyUtils;
import com.bankos.common.response.PageResponse;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.accounts.dto.AccountResponse;
import com.bankos.modules.accounts.dto.CreateAccountRequest;
import com.bankos.modules.accounts.entity.Account;
import com.bankos.modules.accounts.entity.AccountStatus;
import com.bankos.modules.accounts.mapper.AccountMapper;
import com.bankos.modules.accounts.repository.AccountRepository;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.customers.entity.Customer;
import com.bankos.modules.customers.entity.CustomerStatus;
import com.bankos.modules.customers.repository.CustomerRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            AuditService auditService
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorUserId = TenantContext.requireUserId();

        Customer customer = customerRepository.findByIdAndTenantId(request.customerId(), tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.CUSTOMER_NOT_FOUND));
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new ApiException(ErrorCode.CONFLICT, "El cliente no está activo");
        }

        String currency;
        try {
            currency = MoneyUtils.normalizeCurrency(request.currency());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.CURRENCY_NOT_SUPPORTED);
        }

        Account account = new Account();
        account.setTenantId(tenantId);
        account.setCustomerId(customer.getId());
        account.setAccountNumber(generateAccountNumber(tenantId));
        account.setCurrency(currency);
        account.setBalance(MoneyUtils.zeroMoney());
        account.setStatus(AccountStatus.ACTIVE);

        Account saved = accountRepository.save(account);
        auditService.record(
                tenantId,
                actorUserId,
                "ACCOUNT_CREATED",
                "ACCOUNT",
                saved.getId(),
                Map.of(
                        "account_number", saved.getAccountNumber(),
                        "currency", saved.getCurrency(),
                        "customer_id", saved.getCustomerId().toString()
                )
        );
        return AccountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> list(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return PageResponse.from(accountRepository.findAllByTenantId(tenantId, pageable)
                .map(AccountMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAll() {
        UUID tenantId = TenantContext.requireTenantId();
        return accountRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(AccountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND));
        return AccountMapper.toResponse(account);
    }

    private String generateAccountNumber(UUID tenantId) {
        for (int attempt = 0; attempt < 25; attempt++) {
            long value = secureRandom.nextLong(1_000_000_000L, 10_000_000_000L);
            String candidate = Long.toString(value);
            if (!accountRepository.existsByTenantIdAndAccountNumber(tenantId, candidate)) {
                return candidate;
            }
        }
        throw new ApiException(ErrorCode.INTERNAL_ERROR, "No se pudo generar un número de cuenta único");
    }
}
