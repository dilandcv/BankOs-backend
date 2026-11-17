package com.bankos.modules.transactions.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.money.MoneyUtils;
import com.bankos.common.response.ApiResponse;
import com.bankos.common.response.PageResponse;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.common.web.CorrelationIdContext;
import com.bankos.modules.accounts.entity.Account;
import com.bankos.modules.accounts.entity.AccountStatus;
import com.bankos.modules.accounts.repository.AccountRepository;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.idempotency.service.IdempotencyService;
import com.bankos.modules.settings.service.SettingsService;
import com.bankos.modules.transactions.dto.DepositRequest;
import com.bankos.modules.transactions.dto.TransactionEntryResponse;
import com.bankos.modules.transactions.dto.TransactionResponse;
import com.bankos.modules.transactions.dto.TransferPreviewRequest;
import com.bankos.modules.transactions.dto.TransferPreviewResponse;
import com.bankos.modules.transactions.dto.TransferRequest;
import com.bankos.modules.transactions.dto.WithdrawalRequest;
import com.bankos.modules.transactions.entity.EntryDirection;
import com.bankos.modules.transactions.entity.Transaction;
import com.bankos.modules.transactions.entity.TransactionEntry;
import com.bankos.modules.transactions.entity.TransactionStatus;
import com.bankos.modules.transactions.entity.TransactionType;
import com.bankos.modules.transactions.mapper.TransactionMapper;
import com.bankos.modules.transactions.repository.TransactionEntryRepository;
import com.bankos.modules.transactions.repository.TransactionRepository;
import com.bankos.modules.webhooks.service.WebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionService {

    private static final String OPERATION_DEPOSIT = "DEPOSIT";
    private static final String OPERATION_WITHDRAWAL = "WITHDRAWAL";
    private static final String OPERATION_TRANSFER = "TRANSFER";
    private static final String EVENT_TRANSACTION_COMPLETED = "transaction.completed";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionEntryRepository transactionEntryRepository;
    private final SettingsService settingsService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransactionEntryRepository transactionEntryRepository,
            SettingsService settingsService,
            IdempotencyService idempotencyService,
            AuditService auditService,
            WebhookService webhookService,
            ObjectMapper objectMapper
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionEntryRepository = transactionEntryRepository;
        this.settingsService = settingsService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApiResponse<TransactionResponse> deposit(String idempotencyKey, DepositRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String key = requireIdempotencyKey(idempotencyKey);
        IdempotencyService.Start start = idempotencyService.start(
                tenantId,
                OPERATION_DEPOSIT,
                key,
                idempotencyService.hash(request)
        );
        if (start.replayPayload() != null) {
            return replay(start.replayPayload());
        }

        Account account = lockAccount(tenantId, request.accountId());
        validateActive(account);
        String currency = normalizeCurrency(request.currency());
        if (!account.getCurrency().equals(currency)) {
            throw new ApiException(ErrorCode.CURRENCY_MISMATCH);
        }

        BigDecimal amount = positiveAmount(request.amount());
        account.setBalance(MoneyUtils.money(account.getBalance().add(amount)));
        accountRepository.save(account);

        Transaction transaction = newTransaction(
                tenantId,
                TransactionType.DEPOSIT,
                amount,
                currency,
                MoneyUtils.zeroMoney(),
                null,
                null,
                account.getId(),
                cleanDescription(request.description()),
                key
        );
        transaction = transactionRepository.save(transaction);

        TransactionEntry entry = newEntry(
                tenantId,
                transaction.getId(),
                account.getId(),
                EntryDirection.CREDIT,
                amount,
                currency
        );
        entry = transactionEntryRepository.save(entry);
        List<TransactionEntry> entries = List.of(entry);

        ApiResponse<TransactionResponse> response = ApiResponse.success(
                "DEPOSIT_COMPLETED",
                "Depósito realizado correctamente",
                TransactionMapper.toResponse(transaction, entries)
        );
        idempotencyService.complete(start.record(), response);

        auditService.record(
                tenantId,
                TenantContext.requireUserId(),
                "TRANSACTION_DEPOSIT",
                "TRANSACTION",
                transaction.getId(),
                Map.of(
                        "account_id", account.getId().toString(),
                        "amount", amount.toPlainString(),
                        "currency", currency
                )
        );
        webhookService.enqueue(tenantId, EVENT_TRANSACTION_COMPLETED, webhookPayload(transaction, entries));
        return response;
    }

    @Transactional
    public ApiResponse<TransactionResponse> withdrawal(String idempotencyKey, WithdrawalRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String key = requireIdempotencyKey(idempotencyKey);
        IdempotencyService.Start start = idempotencyService.start(
                tenantId,
                OPERATION_WITHDRAWAL,
                key,
                idempotencyService.hash(request)
        );
        if (start.replayPayload() != null) {
            return replay(start.replayPayload());
        }

        Account account = lockAccount(tenantId, request.accountId());
        validateActive(account);
        String currency = normalizeCurrency(request.currency());
        if (!account.getCurrency().equals(currency)) {
            throw new ApiException(ErrorCode.CURRENCY_MISMATCH);
        }

        BigDecimal amount = positiveAmount(request.amount());
        if (account.getBalance().compareTo(amount) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        account.setBalance(MoneyUtils.money(account.getBalance().subtract(amount)));
        accountRepository.save(account);

        Transaction transaction = newTransaction(
                tenantId,
                TransactionType.WITHDRAWAL,
                amount,
                currency,
                MoneyUtils.zeroMoney(),
                null,
                account.getId(),
                null,
                cleanDescription(request.description()),
                key
        );
        transaction = transactionRepository.save(transaction);

        TransactionEntry entry = newEntry(
                tenantId,
                transaction.getId(),
                account.getId(),
                EntryDirection.DEBIT,
                amount,
                currency
        );
        entry = transactionEntryRepository.save(entry);
        List<TransactionEntry> entries = List.of(entry);

        ApiResponse<TransactionResponse> response = ApiResponse.success(
                "WITHDRAWAL_COMPLETED",
                "Retiro realizado correctamente",
                TransactionMapper.toResponse(transaction, entries)
        );
        idempotencyService.complete(start.record(), response);

        auditService.record(
                tenantId,
                TenantContext.requireUserId(),
                "TRANSACTION_WITHDRAWAL",
                "TRANSACTION",
                transaction.getId(),
                Map.of(
                        "account_id", account.getId().toString(),
                        "amount", amount.toPlainString(),
                        "currency", currency
                )
        );
        webhookService.enqueue(tenantId, EVENT_TRANSACTION_COMPLETED, webhookPayload(transaction, entries));
        return response;
    }

    @Transactional
    public ApiResponse<TransactionResponse> transfer(String idempotencyKey, TransferRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String key = requireIdempotencyKey(idempotencyKey);
        IdempotencyService.Start start = idempotencyService.start(
                tenantId,
                OPERATION_TRANSFER,
                key,
                idempotencyService.hash(request)
        );
        if (start.replayPayload() != null) {
            return replay(start.replayPayload());
        }

        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new ApiException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        String requestCurrency = normalizeCurrency(request.currency());
        BigDecimal amount = positiveAmount(request.amount());

        LockedAccounts locked = lockAccountsInOrder(
                tenantId,
                request.sourceAccountId(),
                request.targetAccountId()
        );
        Account source = locked.source();
        Account target = locked.target();
        validateActive(source);
        validateActive(target);

        if (!source.getCurrency().equals(requestCurrency)) {
            throw new ApiException(ErrorCode.CURRENCY_MISMATCH);
        }

        String sourceCurrency = source.getCurrency();
        String targetCurrency = target.getCurrency();
        BigDecimal exchangeRate = settingsService.resolveRate(tenantId, sourceCurrency, targetCurrency);
        BigDecimal fee = settingsService.calculateTransferFee(tenantId, amount, sourceCurrency);
        BigDecimal debitAmount = MoneyUtils.money(amount.add(fee));
        BigDecimal creditAmount = MoneyUtils.money(amount.multiply(exchangeRate));

        if (!MoneyUtils.isPositive(creditAmount)) {
            throw new ApiException(ErrorCode.INVALID_AMOUNT, "El monto convertido es demasiado pequeño");
        }
        if (source.getBalance().compareTo(debitAmount) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        source.setBalance(MoneyUtils.money(source.getBalance().subtract(debitAmount)));
        target.setBalance(MoneyUtils.money(target.getBalance().add(creditAmount)));
        accountRepository.save(source);
        accountRepository.save(target);

        Transaction transaction = newTransaction(
                tenantId,
                TransactionType.TRANSFER,
                amount,
                sourceCurrency,
                fee,
                exchangeRate,
                source.getId(),
                target.getId(),
                cleanDescription(request.description()),
                key
        );
        transaction = transactionRepository.save(transaction);

        TransactionEntry debitEntry = newEntry(
                tenantId,
                transaction.getId(),
                source.getId(),
                EntryDirection.DEBIT,
                debitAmount,
                sourceCurrency
        );
        TransactionEntry creditEntry = newEntry(
                tenantId,
                transaction.getId(),
                target.getId(),
                EntryDirection.CREDIT,
                creditAmount,
                targetCurrency
        );
        debitEntry = transactionEntryRepository.save(debitEntry);
        creditEntry = transactionEntryRepository.save(creditEntry);
        List<TransactionEntry> entries = List.of(debitEntry, creditEntry);

        ApiResponse<TransactionResponse> response = ApiResponse.success(
                "TRANSFER_COMPLETED",
                "Transferencia realizada correctamente",
                TransactionMapper.toResponse(transaction, entries)
        );
        idempotencyService.complete(start.record(), response);

        auditService.record(
                tenantId,
                TenantContext.requireUserId(),
                "TRANSACTION_TRANSFER",
                "TRANSACTION",
                transaction.getId(),
                Map.of(
                        "source_account_id", source.getId().toString(),
                        "target_account_id", target.getId().toString(),
                        "amount", amount.toPlainString(),
                        "fee", fee.toPlainString(),
                        "source_currency", sourceCurrency,
                        "target_currency", targetCurrency
                )
        );
        webhookService.enqueue(tenantId, EVENT_TRANSACTION_COMPLETED, webhookPayload(transaction, entries));
        return response;
    }

    @Transactional(readOnly = true)
    public TransferPreviewResponse preview(TransferPreviewRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new ApiException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        String requestCurrency = normalizeCurrency(request.currency());
        BigDecimal amount = positiveAmount(request.amount());

        Account source = accountRepository.findByIdAndTenantId(request.sourceAccountId(), tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account target = accountRepository.findByIdAndTenantId(request.targetAccountId(), tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND));
        validateActive(source);
        validateActive(target);

        if (!source.getCurrency().equals(requestCurrency)) {
            throw new ApiException(ErrorCode.CURRENCY_MISMATCH);
        }

        BigDecimal exchangeRate = settingsService.resolveRate(
                tenantId,
                source.getCurrency(),
                target.getCurrency()
        );
        BigDecimal fee = settingsService.calculateTransferFee(
                tenantId,
                amount,
                source.getCurrency()
        );
        BigDecimal debitAmount = MoneyUtils.money(amount.add(fee));
        BigDecimal creditAmount = MoneyUtils.money(amount.multiply(exchangeRate));

        return new TransferPreviewResponse(
                source.getId(),
                target.getId(),
                amount,
                source.getCurrency(),
                target.getCurrency(),
                exchangeRate,
                fee,
                debitAmount,
                creditAmount
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> history(
            UUID accountId,
            String type,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        UUID tenantId = TenantContext.requireTenantId();
        if (accountId != null) {
            accountRepository.findByIdAndTenantId(accountId, tenantId)
                    .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND));
        }

        TransactionType transactionType = parseType(type);
        Specification<Transaction> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (accountId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("sourceAccountId"), accountId),
                        cb.equal(root.get("destinationAccountId"), accountId)
                ));
            }
            if (transactionType != null) {
                predicates.add(cb.equal(root.get("type"), transactionType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")
                );
        Page<Transaction> page = transactionRepository.findAll(specification, effectivePageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Transaction transaction = transactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));
        return toResponse(transaction);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        List<TransactionEntry> entries = transactionEntryRepository
                .findAllByTransactionIdAndTenantIdOrderByCreatedAtAsc(transaction.getId(), transaction.getTenantId());
        return TransactionMapper.toResponse(transaction, entries);
    }

    private Transaction newTransaction(
            UUID tenantId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            BigDecimal fee,
            BigDecimal exchangeRate,
            UUID sourceAccountId,
            UUID destinationAccountId,
            String description,
            String idempotencyKey
    ) {
        Transaction transaction = new Transaction();
        transaction.setTenantId(tenantId);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setFee(fee);
        transaction.setExchangeRate(exchangeRate);
        transaction.setSourceAccountId(sourceAccountId);
        transaction.setDestinationAccountId(destinationAccountId);
        transaction.setDescription(description);
        transaction.setCorrelationId(CorrelationIdContext.getOrCreate());
        transaction.setIdempotencyKey(idempotencyKey);
        return transaction;
    }

    private TransactionEntry newEntry(
            UUID tenantId,
            UUID transactionId,
            UUID accountId,
            EntryDirection direction,
            BigDecimal amount,
            String currency
    ) {
        TransactionEntry entry = new TransactionEntry();
        entry.setTenantId(tenantId);
        entry.setTransactionId(transactionId);
        entry.setAccountId(accountId);
        entry.setDirection(direction);
        entry.setAmount(amount);
        entry.setCurrency(currency);
        return entry;
    }

    private Account lockAccount(UUID tenantId, UUID accountId) {
        return accountRepository.findByIdAndTenantIdForUpdate(accountId, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private LockedAccounts lockAccountsInOrder(UUID tenantId, UUID sourceId, UUID targetId) {
        boolean sourceFirst = sourceId.toString().compareTo(targetId.toString()) <= 0;
        UUID firstId = sourceFirst ? sourceId : targetId;
        UUID secondId = sourceFirst ? targetId : sourceId;

        Account first = lockAccount(tenantId, firstId);
        Account second = lockAccount(tenantId, secondId);
        return sourceFirst
                ? new LockedAccounts(first, second)
                : new LockedAccounts(second, first);
    }

    private void validateActive(Account account) {
        if (account.getStatus() == AccountStatus.INACTIVE) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE);
        }
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new ApiException(ErrorCode.ACCOUNT_BLOCKED);
        }
    }

    private BigDecimal positiveAmount(BigDecimal amount) {
        if (!MoneyUtils.isPositive(amount)) {
            throw new ApiException(ErrorCode.INVALID_AMOUNT);
        }
        return MoneyUtils.money(amount);
    }

    private String normalizeCurrency(String currency) {
        try {
            return MoneyUtils.normalizeCurrency(currency);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.CURRENCY_NOT_SUPPORTED);
        }
    }

    private String cleanDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private TransactionType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return TransactionType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "type debe ser DEPOSIT, WITHDRAWAL o TRANSFER");
        }
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        String key = idempotencyKey.trim();
        if (key.length() > 100) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key no puede superar 100 caracteres");
        }
        return key;
    }

    private ApiResponse<TransactionResponse> replay(String payload) {
        try {
            return objectMapper.readValue(
                    payload,
                    new TypeReference<ApiResponse<TransactionResponse>>() {
                    }
            );
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "No se pudo recuperar la respuesta idempotente");
        }
    }

    private Map<String, Object> webhookPayload(Transaction transaction, List<TransactionEntry> entries) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transaction_id", transaction.getId().toString());
        payload.put("type", transaction.getType().name());
        payload.put("status", transaction.getStatus().name());
        payload.put("amount", transaction.getAmount());
        payload.put("currency", transaction.getCurrency());
        payload.put("fee", transaction.getFee());
        payload.put("exchange_rate", transaction.getExchangeRate());
        payload.put("source_account_id", transaction.getSourceAccountId() == null
                ? null
                : transaction.getSourceAccountId().toString());
        payload.put("destination_account_id", transaction.getDestinationAccountId() == null
                ? null
                : transaction.getDestinationAccountId().toString());
        payload.put("description", transaction.getDescription());
        payload.put("correlation_id", transaction.getCorrelationId());
        payload.put("occurred_at", transaction.getCreatedAt() == null ? Instant.now() : transaction.getCreatedAt());
        payload.put("entries", entries.stream().map(entry -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("entry_id", entry.getId().toString());
            value.put("account_id", entry.getAccountId().toString());
            value.put("direction", entry.getDirection().name());
            value.put("amount", entry.getAmount());
            value.put("currency", entry.getCurrency());
            return value;
        }).toList());
        return payload;
    }

    private record LockedAccounts(Account source, Account target) {
    }
}
