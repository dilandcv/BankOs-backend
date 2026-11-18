package com.bankos.modules.auth.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.security.JwtService;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.auth.dto.LoginRequest;
import com.bankos.modules.auth.dto.LoginResponse;
import com.bankos.modules.auth.dto.MeResponse;
import com.bankos.modules.banks.dto.LinkedBankResponse;
import com.bankos.modules.banks.dto.MasterUserSummary;
import com.bankos.modules.banks.service.BankLinkService;
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
    private final BankLinkService bankLinkService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            MasterUserRepository masterUserRepository,
            BankLinkService bankLinkService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.masterUserRepository = masterUserRepository;
        this.bankLinkService = bankLinkService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
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
