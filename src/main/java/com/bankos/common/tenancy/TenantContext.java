package com.bankos.common.tenancy;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.modules.users.entity.Role;

import java.util.Set;
import java.util.UUID;

public final class TenantContext {

    public record Principal(
            UUID tenantId,
            UUID userId,
            String email,
            Set<Role> roles
    ) {
    }

    private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Principal principal) {
        CURRENT.set(principal);
    }

    public static Principal get() {
        return CURRENT.get();
    }

    public static UUID requireTenantId() {
        Principal principal = CURRENT.get();
        if (principal == null || principal.tenantId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return principal.tenantId();
    }

    public static UUID requireUserId() {
        Principal principal = CURRENT.get();
        if (principal == null || principal.userId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }

    public static Set<Role> roles() {
        Principal principal = CURRENT.get();
        return principal == null ? Set.of() : principal.roles();
    }

    public static boolean hasRole(Role role) {
        return roles().contains(role);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
