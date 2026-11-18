package com.bankos.modules.banks.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.security.JwtService;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.banks.dto.AvailableBankResponse;
import com.bankos.modules.banks.dto.ExchangeTokenResponse;
import com.bankos.modules.banks.dto.LinkBankRequest;
import com.bankos.modules.banks.dto.LinkedBankResponse;
import com.bankos.modules.banks.entity.BankAuthStatus;
import com.bankos.modules.banks.entity.UserBankLink;
import com.bankos.modules.banks.repository.UserBankLinkRepository;
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
import com.bankos.modules.users.repository.MasterUserRepository;
import com.bankos.modules.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BankLinkService {

    private final UserBankLinkRepository userBankLinkRepository;
    private final TenantRepository tenantRepository;
    private final MasterUserRepository masterUserRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    public BankLinkService(
            UserBankLinkRepository userBankLinkRepository,
            TenantRepository tenantRepository,
            MasterUserRepository masterUserRepository,
            UserRepository userRepository,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.userBankLinkRepository = userBankLinkRepository;
        this.tenantRepository = tenantRepository;
        this.masterUserRepository = masterUserRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LinkedBankResponse> getLinkedBanks(UUID userId) {
        List<UserBankLink> links = userBankLinkRepository.findAllByUserId(userId);
        Map<UUID, Tenant> tenantsById = tenantRepository.findAll().stream()
                .collect(Collectors.toMap(Tenant::getId, t -> t));

        return links.stream()
                .map(link -> toLinkedResponse(link, tenantsById.get(link.getTenantId())))
                .filter(res -> res != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableBankResponse> getAvailableBanks(UUID userId) {
        Set<UUID> linkedTenantIds = userBankLinkRepository.findAllByUserId(userId).stream()
                .map(UserBankLink::getTenantId)
                .collect(Collectors.toSet());

        return tenantRepository.findAll().stream()
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE && !"PLATFORM".equalsIgnoreCase(t.getCode()))
                .map(t -> new AvailableBankResponse(
                        t.getId(),
                        t.getCode(),
                        t.getName(),
                        t.getPrimaryColor(),
                        t.getSecondaryColor(),
                        t.getLogoUrl(),
                        "OAUTH2",
                        linkedTenantIds.contains(t.getId())
                ))
                .toList();
    }

    @Transactional
    public LinkedBankResponse linkBank(UUID userId, LinkBankRequest request) {
        String code = request.tenantCode().trim().toUpperCase(Locale.ROOT);
        Tenant tenant = tenantRepository.findByCode(code)
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.TENANT_NOT_FOUND, "Institución no encontrada: " + code));

        if ("PLATFORM".equalsIgnoreCase(tenant.getCode())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "No se puede vincular la plataforma como banco");
        }

        if (userBankLinkRepository.existsByUserIdAndTenantId(userId, tenant.getId())) {
            throw new ApiException(ErrorCode.BANK_ALREADY_LINKED);
        }

        List<UserBankLink> existingLinks = userBankLinkRepository.findAllByUserId(userId);
        boolean isFirst = existingLinks.isEmpty();

        String nickname = request.accountNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = tenant.getName() + " Personal";
        }

        String type = request.accountType();
        if (type == null || type.isBlank()) {
            type = "SAVINGS";
        }

        String mask = "•••• " + (1000 + random.nextInt(9000));

        UserBankLink link = new UserBankLink();
        link.setUserId(userId);
        link.setTenantId(tenant.getId());
        link.setAccountNickname(nickname);
        link.setAccountNumberMask(mask);
        link.setAccountType(type);
        link.setAuthStatus(BankAuthStatus.CONNECTED);
        link.setAuthMechanism("OAUTH2");
        link.setDefault(isFirst);
        link.setLastSyncAt(Instant.now());

        UserBankLink saved = userBankLinkRepository.save(link);

        auditService.record(
                tenant.getId(),
                userId,
                "BANK_LINKED",
                "USER_BANK_LINK",
                saved.getId(),
                Map.of("tenant_code", tenant.getCode(), "account_type", type)
        );

        return toLinkedResponse(saved, tenant);
    }

    @Transactional
    public void setDefault(UUID userId, UUID linkId) {
        List<UserBankLink> links = userBankLinkRepository.findAllByUserId(userId);
        UserBankLink target = links.stream()
                .filter(l -> l.getId().equals(linkId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.BANK_LINK_NOT_FOUND));

        for (UserBankLink l : links) {
            l.setDefault(l.getId().equals(linkId));
        }
        userBankLinkRepository.saveAll(links);
    }

    @Transactional
    public void unlinkBank(UUID userId, UUID linkId) {
        UserBankLink target = userBankLinkRepository.findByIdAndUserId(linkId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.BANK_LINK_NOT_FOUND));

        boolean wasDefault = target.isDefault();
        userBankLinkRepository.delete(target);

        if (wasDefault) {
            List<UserBankLink> remaining = userBankLinkRepository.findAllByUserId(userId);
            if (!remaining.isEmpty()) {
                UserBankLink newDefault = remaining.get(0);
                newDefault.setDefault(true);
                userBankLinkRepository.save(newDefault);
            }
        }

        auditService.record(
                target.getTenantId(),
                userId,
                "BANK_UNLINKED",
                "USER_BANK_LINK",
                target.getId(),
                Map.of("link_id", linkId.toString())
        );
    }

    @Transactional
    public LinkedBankResponse reauthorize(UUID userId, UUID linkId) {
        UserBankLink target = userBankLinkRepository.findByIdAndUserId(linkId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.BANK_LINK_NOT_FOUND));

        target.setAuthStatus(BankAuthStatus.CONNECTED);
        target.setLastSyncAt(Instant.now());
        UserBankLink saved = userBankLinkRepository.save(target);

        Tenant tenant = tenantRepository.findById(saved.getTenantId())
                .orElseThrow(() -> new ApiException(ErrorCode.TENANT_NOT_FOUND));

        return toLinkedResponse(saved, tenant);
    }

    @Transactional
    public ExchangeTokenResponse exchangeBankToken(UUID userId, String tenantCode) {
        String code = tenantCode.trim().toUpperCase(Locale.ROOT);
        Tenant tenant = tenantRepository.findByCode(code)
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.TENANT_NOT_FOUND));

        UserBankLink link = userBankLinkRepository.findByUserIdAndTenantId(userId, tenant.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.TENANT_ACCESS_DENIED, "No tienes acceso autorizado a esta institución"));

        if (link.getAuthStatus() == BankAuthStatus.PENDING_REAUTH) {
            throw new ApiException(ErrorCode.BANK_REAUTH_REQUIRED);
        }
        if (link.getAuthStatus() == BankAuthStatus.REVOKED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "La autorización para este banco ha sido revocada");
        }

        link.setLastSyncAt(Instant.now());
        userBankLinkRepository.save(link);

        MasterUser masterUser = masterUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // Busca si el usuario ya tiene un rol institucional en este tenant
        Role role = userRepository.findByTenantIdAndEmail(tenant.getId(), masterUser.getEmail())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .map(User::getRole)
                .orElse(Role.OPERATOR);

        JwtService.IssuedToken issuedToken = jwtService.issueTenant(masterUser, tenant, List.of(role.name()));

        UserSummary userSummary = new UserSummary(masterUser.getId(), masterUser.getEmail(), masterUser.getFullName(), role);
        TenantSummary tenantSummary = TenantMapper.toSummary(tenant);

        return new ExchangeTokenResponse(
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                userSummary,
                tenantSummary
        );
    }

    private LinkedBankResponse toLinkedResponse(UserBankLink link, Tenant tenant) {
        if (tenant == null) return null;
        return new LinkedBankResponse(
                link.getId(),
                tenant.getId(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getPrimaryColor(),
                tenant.getSecondaryColor(),
                tenant.getLogoUrl(),
                link.getAccountNickname(),
                link.getAccountNumberMask(),
                link.getAccountType(),
                link.getAuthStatus().name(),
                link.getAuthMechanism(),
                link.isDefault(),
                link.getLastSyncAt()
        );
    }
}
