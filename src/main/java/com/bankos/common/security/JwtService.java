package com.bankos.common.security;

import com.bankos.modules.tenants.entity.Tenant;
import com.bankos.modules.users.entity.MasterUser;
import com.bankos.modules.users.entity.Role;
import com.bankos.modules.users.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(properties.expirationMs());
        List<String> roles = user.getRole() == null
                ? List.of()
                : List.of(user.getRole()).stream().map(Role::name).collect(Collectors.toList());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("tenant_id", user.getTenantId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt, properties.expirationMs() / 1000);
    }

    public IssuedToken issueMaster(MasterUser masterUser) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(properties.expirationMs());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(masterUser.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("email", masterUser.getEmail())
                .claim("roles", List.of("PLATFORM_USER"))
                .claim("type", "MASTER")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt, properties.expirationMs() / 1000);
    }

    public IssuedToken issueTenant(MasterUser masterUser, Tenant tenant, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(properties.expirationMs());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(masterUser.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("tenant_id", tenant.getId().toString())
                .claim("email", masterUser.getEmail())
                .claim("roles", roles)
                .claim("type", "TENANT")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt, properties.expirationMs() / 1000);
    }

    public record IssuedToken(String token, Instant expiresAt, long expiresInSeconds) {
    }
}
