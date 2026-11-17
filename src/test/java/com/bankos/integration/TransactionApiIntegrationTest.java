package com.bankos.integration;

import com.bankos.modules.accounts.entity.Account;
import com.bankos.modules.accounts.entity.AccountStatus;
import com.bankos.modules.accounts.repository.AccountRepository;
import com.bankos.modules.customers.entity.Customer;
import com.bankos.modules.customers.entity.CustomerStatus;
import com.bankos.modules.customers.repository.CustomerRepository;
import com.bankos.modules.tenants.entity.Tenant;
import com.bankos.modules.tenants.entity.TenantStatus;
import com.bankos.modules.tenants.repository.TenantRepository;
import com.bankos.modules.users.entity.Role;
import com.bankos.modules.users.entity.User;
import com.bankos.modules.users.entity.UserStatus;
import com.bankos.modules.users.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class TransactionApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User user;
    private Account account;
    private String token;

    @BeforeEach
    void setUp() {
        tenant = createTenant("PRIMARY");
        user = createUser(tenant);
        Customer customer = createCustomer(tenant);
        account = createAccount(tenant, customer, "USD", new BigDecimal("1000.0000"));
        token = login(tenant.getCode(), user.getEmail());
    }

    @Test
    void depositUpdatesBalanceAndIsReplayableWithSameIdempotencyKey() {
        String key = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "account_id", account.getId().toString(),
                "amount", "100.00",
                "currency", "USD",
                "description", "Depósito de prueba"
        );

        ResponseEntity<String> first = postDeposit(key, body);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode firstJson = read(first);
        assertThat(firstJson.path("success").asBoolean()).isTrue();
        assertThat(firstJson.path("data").path("amount").asText()).isEqualTo("100.0000");
        String transactionId = firstJson.path("data").path("id").asText();

        ResponseEntity<String> replay = postDeposit(key, body);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(replay).path("data").path("id").asText()).isEqualTo(transactionId);

        ResponseEntity<String> accountResponse = restTemplate.exchange(
                "/api/v1/accounts/" + account.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(accountResponse).path("data").path("balance").asText()).isEqualTo("1100.0000");
    }

    @Test
    void sameIdempotencyKeyWithDifferentBodyIsRejected() {
        String key = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "account_id", account.getId().toString(),
                "amount", "100.00",
                "currency", "USD",
                "description", "Primera solicitud"
        );
        assertThat(postDeposit(key, body).getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> differentBody = Map.of(
                "account_id", account.getId().toString(),
                "amount", "50.00",
                "currency", "USD",
                "description", "Segunda solicitud"
        );
        ResponseEntity<String> conflict = postDeposit(key, differentBody);

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(read(conflict).path("code").asText()).isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void transactionHistorySupportsOptionalFilters() {
        String key = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "account_id", account.getId().toString(),
                "amount", "25.00",
                "currency", "USD",
                "description", "Historial"
        );
        ResponseEntity<String> deposit = postDeposit(key, body);
        assertThat(deposit.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> all = restTemplate.exchange(
                "/api/v1/transactions?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(all).path("data").path("total_elements").asLong()).isGreaterThanOrEqualTo(1);

        ResponseEntity<String> filtered = restTemplate.exchange(
                "/api/v1/transactions?page=0&size=10&accountId=" + account.getId() + "&type=DEPOSIT",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
        assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(filtered).path("data").path("items").get(0).path("type").asText())
                .isEqualTo("DEPOSIT");
    }

    @Test
    void tenantCannotReadAccountFromAnotherTenant() {
        Tenant otherTenant = createTenant("OTHER");
        User otherUser = createUser(otherTenant);
        String otherToken = login(otherTenant.getCode(), otherUser.getEmail());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/accounts/" + account.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(otherToken)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(read(response).path("code").asText()).isEqualTo("ACCOUNT_NOT_FOUND");
    }

    private ResponseEntity<String> postDeposit(String idempotencyKey, Map<String, Object> body) {
        HttpHeaders headers = authHeaders(token);
        headers.set("Idempotency-Key", idempotencyKey);
        return restTemplate.exchange(
                "/api/v1/transactions/deposits",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
    }

    private HttpHeaders authHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private JsonNode read(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception ex) {
            throw new AssertionError("Respuesta JSON inválida: " + response.getBody(), ex);
        }
    }

    private String login(String tenantCode, String email) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                Map.of(
                        "tenant_code", tenantCode,
                        "email", email,
                        "password", "Admin123!"
                ),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return read(response).path("data").path("access_token").asText();
    }

    private Tenant createTenant(String prefix) {
        Tenant value = new Tenant();
        value.setCode(("TEST_" + prefix + "_" + randomSuffix()).toUpperCase(Locale.ROOT));
        value.setName("Tenant " + prefix);
        value.setStatus(TenantStatus.ACTIVE);
        value.setPrimaryColor("#123456");
        value.setSecondaryColor("#654321");
        return tenantRepository.save(value);
    }

    private User createUser(Tenant owner) {
        User value = new User();
        value.setTenantId(owner.getId());
        value.setEmail(("user_" + randomSuffix() + "@test.local").toLowerCase(Locale.ROOT));
        value.setPasswordHash(passwordEncoder.encode("Admin123!"));
        value.setFullName("Test Admin");
        value.setRole(Role.TENANT_ADMIN);
        value.setStatus(UserStatus.ACTIVE);
        return userRepository.save(value);
    }

    private Customer createCustomer(Tenant owner) {
        Customer value = new Customer();
        value.setTenantId(owner.getId());
        value.setDocumentType("CC");
        value.setDocumentNumber("DOC_" + randomSuffix());
        value.setFullName("Cliente de prueba");
        value.setStatus(CustomerStatus.ACTIVE);
        return customerRepository.save(value);
    }

    private Account createAccount(Tenant owner, Customer customer, String currency, BigDecimal balance) {
        Account value = new Account();
        value.setTenantId(owner.getId());
        value.setCustomerId(customer.getId());
        value.setAccountNumber(String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L)));
        value.setCurrency(currency);
        value.setBalance(balance);
        value.setStatus(AccountStatus.ACTIVE);
        return accountRepository.save(value);
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
