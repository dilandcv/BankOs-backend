package com.bankos.common.tenancy;

import com.bankos.modules.users.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                String tenantId = jwtAuthentication.getToken().getClaimAsString("tenant_id");
                String email = jwtAuthentication.getToken().getClaimAsString("email");
                List<String> roles = jwtAuthentication.getToken().getClaimAsStringList("roles");

                Set<Role> parsedRoles = roles == null
                        ? Set.of()
                        : roles.stream()
                                .map(r -> {
                                    try {
                                        return Role.valueOf(r);
                                    } catch (IllegalArgumentException e) {
                                        return null;
                                    }
                                })
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet());

                UUID parsedTenantId = (tenantId != null && !tenantId.isBlank()) ? UUID.fromString(tenantId) : null;
                UUID userId = UUID.fromString(jwtAuthentication.getToken().getSubject());

                TenantContext.set(new TenantContext.Principal(
                        parsedTenantId,
                        userId,
                        email,
                        parsedRoles
                ));
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
