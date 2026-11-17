package com.bankos.modules.users.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.response.PageResponse;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.users.dto.CreateUserRequest;
import com.bankos.modules.users.dto.UserResponse;
import com.bankos.modules.users.entity.Role;
import com.bankos.modules.users.entity.User;
import com.bankos.modules.users.entity.UserStatus;
import com.bankos.modules.users.mapper.UserMapper;
import com.bankos.modules.users.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorUserId = TenantContext.requireUserId();

        if (request.role() == Role.PLATFORM_ADMIN && !TenantContext.hasRole(Role.PLATFORM_ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Solo un administrador de plataforma puede crear PLATFORM_ADMIN");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new ApiException(ErrorCode.CONFLICT, "Ya existe un usuario con ese email en la institución");
        }

        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        auditService.record(
                tenantId,
                actorUserId,
                "USER_CREATED",
                "USER",
                saved.getId(),
                Map.of("email", saved.getEmail(), "role", saved.getRole().name())
        );
        return UserMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return PageResponse.from(userRepository.findAllByTenantId(tenantId, pageable)
                .map(UserMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return UserMapper.toResponse(user);
    }
}
