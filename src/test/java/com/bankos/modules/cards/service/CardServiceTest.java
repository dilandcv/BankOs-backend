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
import com.bankos.modules.cards.repository.CardRepository;
import com.bankos.modules.customers.entity.Customer;
import com.bankos.modules.customers.repository.CustomerRepository;
import com.bankos.modules.users.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CardService cardService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.Principal(tenantId, userId, "user@test.local", Set.of(Role.OPERATOR)));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("createCard debe emitir tarjeta activa y registrar auditoría")
    void createCardSuccess() {
        Account account = new Account();
        account.setId(accountId);
        account.setTenantId(tenantId);
        account.setCustomerId(customerId);

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setTenantId(tenantId);
        customer.setFullName("Juan Pérez");

        when(accountRepository.findByIdAndTenantId(accountId, tenantId)).thenReturn(Optional.of(account));
        when(customerRepository.findByIdAndTenantId(customerId, tenantId)).thenReturn(Optional.of(customer));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CreateCardRequest request = new CreateCardRequest(accountId, customerId, "Juan Pérez", CardType.DEBIT);
        CardResponse response = cardService.createCard(request);

        assertThat(response).isNotNull();
        assertThat(response.cardNumber()).startsWith("•••• •••• •••• ");
        assertThat(response.cardNumber()).hasSize(19);
        assertThat(response.cardholderName()).isEqualTo("Juan Pérez");
        assertThat(response.cvvMasked()).isEqualTo("•••");
        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.cardType()).isEqualTo(CardType.DEBIT);

        verify(auditService).record(eq(tenantId), eq(userId), eq("CARD_CREATED"), eq("CARD"), any(), any());
    }

    @Test
    @DisplayName("updateStatus debe cambiar estado a BLOCKED y auditar")
    void updateStatusSuccess() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card();
        card.setId(cardId);
        card.setTenantId(tenantId);
        card.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findByIdAndTenantId(cardId, tenantId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.BLOCKED);
        CardResponse response = cardService.updateStatus(cardId, request);

        assertThat(response.status()).isEqualTo(CardStatus.BLOCKED);
        verify(auditService).record(eq(tenantId), eq(userId), eq("CARD_STATUS_UPDATED"), eq("CARD"), eq(cardId), any());
    }

    @Test
    @DisplayName("updateStatus con tarjeta inexistente debe lanzar ApiException CARD_NOT_FOUND")
    void updateStatusNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findByIdAndTenantId(cardId, tenantId)).thenReturn(Optional.empty());

        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.BLOCKED);
        assertThatThrownBy(() -> cardService.updateStatus(cardId, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).errorCode()).isEqualTo(ErrorCode.CARD_NOT_FOUND));
    }

    @Test
    @DisplayName("listCards filtra por accountId cuando se especifica")
    void listCardsByAccountId() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setTenantId(tenantId);
        card.setAccountId(accountId);
        card.setCardNumber("•••• •••• •••• 1234");
        card.setCardholderName("Ana Gómez");
        card.setExpirationDate("12/28");
        card.setCvvMasked("•••");
        card.setStatus(CardStatus.ACTIVE);
        card.setCardType(CardType.DEBIT);

        when(cardRepository.findAllByTenantIdAndAccountId(tenantId, accountId)).thenReturn(List.of(card));

        List<CardResponse> cards = cardService.listCards(accountId, null);
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).cardNumber()).isEqualTo("•••• •••• •••• 1234");
    }
}
