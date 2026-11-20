package com.bankos.modules.auth.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.security.JwtService;
import com.bankos.modules.accounts.entity.Account;
import com.bankos.modules.accounts.repository.AccountRepository;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.auth.dto.LoginResponse;
import com.bankos.modules.auth.dto.RegisterRequest;
import com.bankos.modules.banks.service.BankLinkService;
import com.bankos.modules.cards.entity.Card;
import com.bankos.modules.cards.entity.CardType;
import com.bankos.modules.cards.service.CardService;
import com.bankos.modules.customers.entity.Customer;
import com.bankos.modules.customers.repository.CustomerRepository;
import com.bankos.modules.tenants.entity.Tenant;
import com.bankos.modules.tenants.entity.TenantStatus;
import com.bankos.modules.tenants.repository.TenantRepository;
import com.bankos.modules.users.entity.Role;
import com.bankos.modules.users.entity.User;
import com.bankos.modules.users.repository.MasterUserRepository;
import com.bankos.modules.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MasterUserRepository masterUserRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardService cardService;

    @Mock
    private BankLinkService bankLinkService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register exitoso debe crear User, Customer, Account, Card y emitir JWT")
    void registerSuccess() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setCode("BANCOLOMBIA");
        tenant.setName("Bancolombia");
        tenant.setStatus(TenantStatus.ACTIVE);

        when(tenantRepository.findByCode("BANCOLOMBIA")).thenReturn(Optional.of(tenant));
        when(userRepository.existsByTenantIdAndEmail(eq(tenantId), anyString())).thenReturn(false);
        when(customerRepository.existsByTenantIdAndDocumentNumber(eq(tenantId), anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        Card card = new Card();
        card.setId(UUID.randomUUID());
        when(cardService.issueInitialCard(eq(tenantId), any(), any(), eq("Carlos Ruiz"), eq(CardType.DEBIT), any()))
                .thenReturn(card);

        when(jwtService.issue(any(User.class))).thenReturn(new JwtService.IssuedToken("jwt-token-xyz", java.time.Instant.now().plusSeconds(3600), 3600L));

        RegisterRequest request = new RegisterRequest(
                "BANCOLOMBIA",
                "carlos@bancolombia.local",
                "Password123!",
                "Carlos Ruiz",
                "CC",
                "987654321"
        );

        LoginResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt-token-xyz");
        assertThat(response.user().email()).isEqualTo("carlos@bancolombia.local");
        assertThat(response.user().role()).isEqualTo(Role.OPERATOR);
        assertThat(response.tenant().code()).isEqualTo("BANCOLOMBIA");

        verify(auditService).record(eq(tenantId), any(), eq("USER_REGISTERED"), eq("USER"), any(), any());
    }

    @Test
    @DisplayName("register con email duplicado en el tenant debe fallar con CONFLICT")
    void registerDuplicateEmailFails() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setCode("BANCOLOMBIA");
        tenant.setStatus(TenantStatus.ACTIVE);

        when(tenantRepository.findByCode("BANCOLOMBIA")).thenReturn(Optional.of(tenant));
        when(userRepository.existsByTenantIdAndEmail(tenantId, "existente@bancolombia.local")).thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "BANCOLOMBIA",
                "existente@bancolombia.local",
                "Password123!",
                "Carlos Ruiz",
                "CC",
                "987654321"
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    @DisplayName("register con documento duplicado en el tenant debe fallar con CONFLICT")
    void registerDuplicateDocumentFails() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setCode("BANCOLOMBIA");
        tenant.setStatus(TenantStatus.ACTIVE);

        when(tenantRepository.findByCode("BANCOLOMBIA")).thenReturn(Optional.of(tenant));
        when(userRepository.existsByTenantIdAndEmail(tenantId, "nuevo@bancolombia.local")).thenReturn(false);
        when(customerRepository.existsByTenantIdAndDocumentNumber(tenantId, "123456")).thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "BANCOLOMBIA",
                "nuevo@bancolombia.local",
                "Password123!",
                "Carlos Ruiz",
                "CC",
                "123456"
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));
    }
}
