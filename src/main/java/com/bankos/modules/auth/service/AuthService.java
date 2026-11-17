package com.bankos.modules.auth.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.auth.dto.LoginRequest;
import com.bankos.modules.auth.dto.LoginResponse;
import com.bankos.modules.auth.dto.MeResponse;
import com.bankos.modules.tenants.entity.Tenant;
import com.bankos.modules.tenants.entity.TenantStatus;
import com.bankos.modules.tenants.mapper.TenantMapper;
import com.bankos.modules.tenants.repository.TenantRepository;
import com.bankos.modules.users.entity.User;
import com.bankos.modules.users.entity.UserStatus;
import com.bankos.modules.users.mapper.UserMapper;
import com.bankos.modules.users.repository.UserRepository;
import com.bankos.common.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String tenantCode = request.tenantCode().trim().toUpperCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Tenant tenant = tenantRepository.findByCode(tenantCode)
                .filter(value -> value.getStatus() == TenantStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        User user = userRepository.findByTenantIdAndEmail(tenant.getId(), email)
                .filter(value -> value.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

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

    @Transactional(readOnly = true)
    public MeResponse me() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.TENANT_NOT_FOUND));

        return new MeResponse(UserMapper.toSummary(user), TenantMapper.toSummary(tenant));
    }
}
