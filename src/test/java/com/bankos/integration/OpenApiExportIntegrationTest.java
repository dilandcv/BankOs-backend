package com.bankos.integration;

import com.bankos.modules.tenants.entity.Tenant;
import com.bankos.modules.tenants.entity.TenantStatus;
import com.bankos.modules.tenants.repository.TenantRepository;
import com.bankos.modules.users.entity.Role;
import com.bankos.modules.users.entity.User;
import com.bankos.modules.users.entity.UserStatus;
import com.bankos.modules.users.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OpenApiExportIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Test
    void exportOpenApiYaml() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setCode("OPENAPI_TEST_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT));
        tenant.setName("OpenAPI Test Bank");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setPrimaryColor("#003366");
        tenant.setSecondaryColor("#006699");
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setTenantId(tenant.getId());
        user.setEmail("openapi_" + UUID.randomUUID().toString().substring(0, 6) + "@test.local");
        user.setPasswordHash(passwordEncoder.encode("Admin123!"));
        user.setFullName("OpenAPI Admin");
        user.setRole(Role.TENANT_ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                Map.of(
                        "tenant_code", tenant.getCode(),
                        "email", user.getEmail(),
                        "password", "Admin123!"
                ),
                String.class
        );
        assertThat(loginResponse.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode loginJson = objectMapper.readTree(loginResponse.getBody());
        String token = loginJson.path("data").path("access_token").asText();
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // Intentar obtener /v3/api-docs.yaml
        ResponseEntity<String> response = restTemplate.exchange(
                "/v3/api-docs.yaml",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String yamlContent;
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            yamlContent = response.getBody();
        } else {
            // Si springdoc expone JSON en /v3/api-docs, obtener JSON y convertir a YAML
            ResponseEntity<String> jsonResponse = restTemplate.exchange(
                    "/v3/api-docs",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            System.out.println("JSON response status: " + jsonResponse.getStatusCode());
            assertThat(jsonResponse.getStatusCode().is2xxSuccessful()).isTrue();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse.getBody());
            com.fasterxml.jackson.dataformat.yaml.YAMLMapper yamlMapper = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper();
            yamlContent = yamlMapper.writeValueAsString(jsonNode);
        }

        assertThat(yamlContent).isNotBlank();
        assertThat(yamlContent).contains("openapi: 3.");
        assertThat(yamlContent).contains("/api/v1/auth/login");

        Path targetPath = Paths.get("/workspace/openapi.yaml");
        Files.writeString(targetPath, yamlContent);
        System.out.println(">>> OpenAPI YAML exportado exitosamente a: " + targetPath.toAbsolutePath() + " (longitud: " + yamlContent.length() + " caracteres)");
    }
}
