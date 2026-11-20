package com.bankos.modules.auth.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.money.MoneyUtils;
import com.bankos.common.security.JwtService;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.accounts.entity.Account;
import com.bankos.modules.accounts.entity.AccountStatus;
import com.bankos.modules.accounts.repository.AccountRepository;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.auth.dto.LoginRequest;
import com.bankos.modules.auth.dto.LoginResponse;
import com.bankos.modules.auth.dto.MeResponse;
import com.bankos.modules.auth.dto.RegisterRequest;
import com.bankos.modules.banks.dto.LinkedBankResponse;
import com.bankos.modules.banks.dto.MasterUserSummary;
import com.bankos.modules.banks.service.BankLinkService;
import com.bankos.modules.cards.entity.Card;
import com.bankos.modules.cards.entity.CardType;
import com.bankos.modules.cards.service.CardService;
import com.bankos.modules.customers.entity.Customer;
import com.bankos.modules.customers.entity.CustomerStatus;
import com.bankos.modules.customers.repository.CustomerRepository;
import com.bankos.modules.tenants.dto.TenantSummary;
import com.bankos.modules.tenants.entity.Tenant;
import com.bankos.modules.tenants.entity.TenantStatus;
import com.bankos.modules.tenants.mapper.TenantMapper;
import com.bankos.modules.tenants.repository.TenantRepository;
import com.bankos.modules.users.dto.UserSummary;
import com.bankos.modules.users.entity.MasterUser;
import com.bankos.modules.users.entity.Role;
import com.bankos.modules.users.entity.User;
import com.bankos.modules.users.entity.UserStatus;
import com.bankos.modules.users.mapper.UserMapper;
import com.bankos.modules.users.repository.MasterUserRepository;
import com.bankos.modules.users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final MasterUserRepository masterUserRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final CardService cardService;
    private final BankLinkService bankLinkService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            MasterUserRepository masterUserRepository,
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            CardService cardService,
            BankLinkService bankLinkService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.masterUserRepository = masterUserRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.cardService = cardService;
        this.bankLinkService = bankLinkService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String tenantCode = request.tenantCode().trim().toUpperCase(Locale.ROOT);
        Tenant tenant = tenantRepository.findByCode(tenantCode)
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.TENANT_NOT_FOUND, "Institución no encontrada o inactiva"));

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByTenantIdAndEmail(tenant.getId(), email)) {
            throw new ApiException(ErrorCode.CONFLICT, "Ya existe un usuario con este correo en la institución");
        }

        String documentNumber = request.documentNumber().trim();
        if (customerRepository.existsByTenantIdAndDocumentNumber(tenant.getId(), documentNumber)) {
            throw new ApiException(ErrorCode.CONFLICT, "Ya existe un cliente registrado con este documento en la institución");
        }

        // 1. Crear User en el tenant con rol OPERATOR
        User user = new User();
        user.setTenantId(tenant.getId());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(Role.OPERATOR);
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        // 2. Crear Customer asociado
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setUserId(savedUser.getId());
        customer.setDocumentType(request.documentType().trim().toUpperCase(Locale.ROOT));
        customer.setDocumentNumber(documentNumber);
        customer.setFullName(request.fullName().trim());
        customer.setStatus(CustomerStatus.ACTIVE);
        Customer savedCustomer = customerRepository.save(customer);

        // 3. Crear Account de ahorros con saldo de bienvenida
        String currency = "BANCO_HORIZONTE".equalsIgnoreCase(tenantCode) || "PAYPAL".equalsIgnoreCase(tenantCode)
                ? "USD"
                : "COP";
        BigDecimal welcomeBalance = "USD".equals(currency)
                ? MoneyUtils.money(new BigDecimal("1000.0000"))
                : MoneyUtils.money(new BigDecimal("100000.0000"));

        Account account = new Account();
        account.setTenantId(tenant.getId());
        account.setCustomerId(savedCustomer.getId());
        account.setAccountNumber(generateUniqueAccountNumber(tenant.getId()));
        account.setCurrency(currency);
        account.setBalance(welcomeBalance);
        account.setStatus(AccountStatus.ACTIVE);
        Account savedAccount = accountRepository.save(account);

        // 4. Emitir primera tarjeta digital
        Card initialCard = cardService.issueInitialCard(
                tenant.getId(),
                savedAccount.getId(),
                savedCustomer.getId(),
                savedCustomer.getFullName(),
                CardType.DEBIT,
                savedUser.getId()
        );

        // 5. Registrar auditoría de registro
        auditService.record(
                tenant.getId(),
                savedUser.getId(),
                "USER_REGISTERED",
                "USER",
                savedUser.getId(),
                Map.of(
                        "email", savedUser.getEmail(),
                        "tenant_code", tenant.getCode(),
                        "account_id", savedAccount.getId().toString(),
                        "customer_id", savedCustomer.getId().toString(),
                        "card_id", initialCard.getId().toString()
                )
        );

        // 6. Emitir JWT y devolver LoginResponse
        JwtService.IssuedToken issuedToken = jwtService.issue(savedUser);
        return new LoginResponse(
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                UserMapper.toSummary(savedUser),
                TenantMapper.toSummary(tenant)
        );
    }

    private String generateUniqueAccountNumber(UUID tenantId) {
        for (int attempt = 0; attempt < 25; attempt++) {
            long value = secureRandom.nextLong(1_000_000_000L, 10_000_000_000L);
            String candidate = Long.toString(value);
            if (!accountRepository.existsByTenantIdAndAccountNumber(tenantId, candidate)) {
                return candidate;
            }
        }
        throw new ApiException(ErrorCode.INTERNAL_ERROR, "No se pudo generar un número de cuenta único");
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // Si no se especifica tenant_code, es un login centralizado de usuario maestro
        if (request.tenantCode() == null || request.tenantCode().isBlank()) {
            return loginMaster(email, request.password());
        }

        // Si se especifica tenant_code, intenta login institucional o delega a master si coincide
        String tenantCode = request.tenantCode().trim().toUpperCase(Locale.ROOT);
        Optional<Tenant> tenantOpt = tenantRepository.findByCode(tenantCode)
                .filter(value -> value.getStatus() == TenantStatus.ACTIVE);

        if (tenantOpt.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        Tenant tenant = tenantOpt.get();

        Optional<User> userOpt = userRepository.findByTenantIdAndEmail(tenant.getId(), email)
                .filter(value -> value.getStatus() == UserStatus.ACTIVE);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
            }

            JwtService.IssuedToken issuedToken = jwtService.issue(user);
            auditService.record(
                    tenant.getId(),
                    user.getId(),
                    "AUTH_LOGIN",
                    "USER",
                    user.getId(),
                    Map.of("email", user.getEmail(), "tenant_code", tenant.getCode())
            );

            return new LoginResponse(
                    issuedToken.token(),
                    "Bearer",
                    issuedToken.expiresInSeconds(),
                    UserMapper.toSummary(user),
                    TenantMapper.toSummary(tenant)
            );
        }

        // Si no existe como usuario institucional, verifica si es usuario maestro con esa institución vinculada
        return loginMaster(email, request.password());
    }

    private LoginResponse loginMaster(String email, String rawPassword) {
        MasterUser masterUser = masterUserRepository.findByEmail(email)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(rawPassword, masterUser.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        JwtService.IssuedToken issuedToken = jwtService.issueMaster(masterUser);
        List<LinkedBankResponse> linkedBanks = bankLinkService.getLinkedBanks(masterUser.getId());

        Tenant platformTenant = tenantRepository.findByCode("PLATFORM")
                .orElseGet(() -> tenantRepository.findAll().stream().findFirst().orElse(null));

        if (platformTenant != null) {
            auditService.record(
                    platformTenant.getId(),
                    null,
                    "AUTH_MASTER_LOGIN",
                    "MASTER_USER",
                    masterUser.getId(),
                    Map.of("email", masterUser.getEmail())
            );
        }

        MasterUserSummary masterSummary = new MasterUserSummary(
                masterUser.getId(),
                masterUser.getEmail(),
                masterUser.getFullName(),
                masterUser.getPhone()
        );

        UserSummary fallbackUserSummary = new UserSummary(
                masterUser.getId(),
                masterUser.getEmail(),
                masterUser.getFullName(),
                Role.OPERATOR
        );

        TenantSummary defaultTenant = null;
        if (!linkedBanks.isEmpty()) {
            LinkedBankResponse def = linkedBanks.stream().filter(LinkedBankResponse::isDefault).findFirst()
                    .orElse(linkedBanks.get(0));
            defaultTenant = new TenantSummary(
                    def.tenantId(),
                    def.tenantCode(),
                    def.tenantName(),
                    def.primaryColor(),
                    def.secondaryColor(),
                    def.logoUrl()
            );
        }

        return new LoginResponse(
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                fallbackUserSummary,
                defaultTenant,
                masterSummary,
                linkedBanks
        );
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        UUID tenantId = TenantContext.get() != null ? TenantContext.get().tenantId() : null;
        UUID userId = TenantContext.requireUserId();

        if (tenantId == null) {
            MasterUser masterUser = masterUserRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

            UserSummary userSummary = new UserSummary(
                    masterUser.getId(),
                    masterUser.getEmail(),
                    masterUser.getFullName(),
                    Role.OPERATOR
            );

            Tenant platformTenant = tenantRepository.findByCode("PLATFORM")
                    .orElseGet(() -> tenantRepository.findAll().stream().findFirst().orElse(null));

            TenantSummary tenantSummary = platformTenant != null
                    ? TenantMapper.toSummary(platformTenant)
                    : new TenantSummary(UUID.randomUUID(), "PLATFORM", "BankOS Platform", "#0B1F3A", "#0070BA", null);

            return new MeResponse(userSummary, tenantSummary);
        }

        Optional<User> userOpt = userRepository.findByIdAndTenantId(userId, tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.TENANT_NOT_FOUND));

        if (userOpt.isPresent()) {
            return new MeResponse(UserMapper.toSummary(userOpt.get()), TenantMapper.toSummary(tenant));
        }

        // Si el userId corresponde a un MasterUser operando en este tenant
        MasterUser masterUser = masterUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        UserSummary userSummary = new UserSummary(
                masterUser.getId(),
                masterUser.getEmail(),
                masterUser.getFullName(),
                Role.OPERATOR
        );

        return new MeResponse(userSummary, TenantMapper.toSummary(tenant));
    }
}
