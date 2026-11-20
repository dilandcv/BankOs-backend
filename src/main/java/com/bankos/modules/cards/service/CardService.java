package com.bankos.modules.cards.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.accounts.entity.Account;
import com.bankos.modules.accounts.repository.AccountRepository;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.cards.dto.CardResponse;
import com.bankos.modules.cards.dto.CreateCardRequest;
import com.bankos.modules.cards.dto.UpdateCardStatusRequest;
import com.bankos.modules.cards.entity.Card;
import com.bankos.modules.cards.entity.CardStatus;
import com.bankos.modules.cards.entity.CardType;
import com.bankos.modules.cards.mapper.CardMapper;
import com.bankos.modules.cards.repository.CardRepository;
import com.bankos.modules.customers.entity.Customer;
import com.bankos.modules.customers.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public CardService(
            CardRepository cardRepository,
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            AuditService auditService
    ) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> listCards(UUID accountId, UUID customerId) {
        UUID tenantId = TenantContext.requireTenantId();

        if (accountId != null) {
            return cardRepository.findAllByTenantIdAndAccountId(tenantId, accountId)
                    .stream()
                    .map(CardMapper::toResponse)
                    .toList();
        }

        if (customerId != null) {
            return cardRepository.findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId)
                    .stream()
                    .map(CardMapper::toResponse)
                    .toList();
        }

        UUID userId = TenantContext.requireUserId();
        Optional<Customer> customerOpt = customerRepository.findByTenantIdAndUserId(tenantId, userId);
        if (customerOpt.isPresent()) {
            return cardRepository.findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerOpt.get().getId())
                    .stream()
                    .map(CardMapper::toResponse)
                    .toList();
        }

        return cardRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(CardMapper::toResponse)
                .toList();
    }

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorUserId = TenantContext.requireUserId();

        Account account = accountRepository.findByIdAndTenantId(request.accountId(), tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND));

        UUID customerId = request.customerId() != null ? request.customerId() : account.getCustomerId();
        Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.CUSTOMER_NOT_FOUND));

        String cardholderName = (request.cardholderName() != null && !request.cardholderName().isBlank())
                ? request.cardholderName().trim()
                : customer.getFullName();

        CardType cardType = request.cardType() != null ? request.cardType() : CardType.DEBIT;

        Card card = issueCardInternal(tenantId, account.getId(), customer.getId(), cardholderName, cardType, actorUserId);
        return CardMapper.toResponse(card);
    }

    @Transactional
    public Card issueInitialCard(
            UUID tenantId,
            UUID accountId,
            UUID customerId,
            String cardholderName,
            CardType cardType,
            UUID actorUserId
    ) {
        return issueCardInternal(tenantId, accountId, customerId, cardholderName, cardType, actorUserId);
    }

    @Transactional
    public CardResponse updateStatus(UUID id, UpdateCardStatusRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorUserId = TenantContext.requireUserId();

        Card card = cardRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.CARD_NOT_FOUND));

        card.setStatus(request.status());
        Card updated = cardRepository.save(card);

        auditService.record(
                tenantId,
                actorUserId,
                "CARD_STATUS_UPDATED",
                "CARD",
                updated.getId(),
                Map.of("status", updated.getStatus().name())
        );

        return CardMapper.toResponse(updated);
    }

    private Card issueCardInternal(
            UUID tenantId,
            UUID accountId,
            UUID customerId,
            String cardholderName,
            CardType cardType,
            UUID actorUserId
    ) {
        int last4 = secureRandom.nextInt(1000, 10000);
        String cardNumber = "•••• •••• •••• " + last4;
        String cvvMasked = "•••";

        LocalDate expirationDateValue = LocalDate.now().plusYears(4);
        String expirationDate = DateTimeFormatter.ofPattern("MM/yy").format(expirationDateValue);

        Card card = new Card();
        card.setTenantId(tenantId);
        card.setAccountId(accountId);
        card.setCustomerId(customerId);
        card.setCardNumber(cardNumber);
        card.setCardholderName(cardholderName);
        card.setExpirationDate(expirationDate);
        card.setCvvMasked(cvvMasked);
        card.setCardType(cardType);
        card.setStatus(CardStatus.ACTIVE);

        Card saved = cardRepository.save(card);

        auditService.record(
                tenantId,
                actorUserId,
                "CARD_CREATED",
                "CARD",
                saved.getId(),
                Map.of(
                        "card_number", saved.getCardNumber(),
                        "card_type", saved.getCardType().name(),
                        "account_id", saved.getAccountId().toString()
                )
        );

        return saved;
    }
}
