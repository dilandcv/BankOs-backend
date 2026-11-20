package com.bankos.modules.tenants.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.common.tenancy.TenantContext;
import com.bankos.modules.audit.service.AuditService;
import com.bankos.modules.tenants.dto.TenantResponse;
import com.bankos.modules.tenants.dto.TenantSummary;
import com.bankos.modules.tenants.dto.UpdateBrandingRequest;
import com.bankos.modules.tenants.entity.Tenant;
import com.bankos.modules.tenants.entity.TenantStatus;
import com.bankos.modules.tenants.repository.TenantRepository;
import com.bankos.modules.users.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TenantService tenantService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.Principal(tenantId, userId, "admin@bank.local", Set.of(Role.TENANT_ADMIN)));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("listPublic devuelve todos los tenants activos sin requerir autenticación")
    void listPublicReturnsActiveTenants() {
        Tenant t1 = new Tenant();
        t1.setId(UUID.randomUUID());
        t1.setCode("BANCO1");
        t1.setName("Banco 1");
        t1.setStatus(TenantStatus.ACTIVE);

        when(tenantRepository.findAllByStatusOrderByNameAsc(TenantStatus.ACTIVE)).thenReturn(List.of(t1));

        List<TenantSummary> result = tenantService.listPublic();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("BANCO1");
    }

    @Test
    @DisplayName("updateBranding actualiza colores y logoUrl y registra auditoría")
    void updateBrandingSuccess() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setCode("BANCO1");
        tenant.setName("Banco 1");
        tenant.setStatus(TenantStatus.ACTIVE);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateBrandingRequest request = new UpdateBrandingRequest("#FF0000", "#00FF00", "https://cdn.bank.com/logo.png");
        TenantResponse response = tenantService.updateBranding(tenantId, request);

        assertThat(response.primaryColor()).isEqualTo("#FF0000");
        assertThat(response.secondaryColor()).isEqualTo("#00FF00");
        assertThat(response.logoUrl()).isEqualTo("https://cdn.bank.com/logo.png");

        verify(auditService).record(eq(tenantId), eq(userId), eq("TENANT_BRANDING_UPDATED"), eq("TENANT"), eq(tenantId), any());
    }

    @Test
    @DisplayName("uploadLogo guarda el archivo localmente y actualiza tenant.logoUrl")
    void uploadLogoSuccess() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setCode("BANCO1");
        tenant.setName("Banco 1");
        tenant.setStatus(TenantStatus.ACTIVE);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                "fake-png-content".getBytes()
        );

        TenantResponse response = tenantService.uploadLogo(tenantId, file);

        assertThat(response.logoUrl()).startsWith("/uploads/logos/");
        assertThat(response.logoUrl()).endsWith(".png");

        verify(auditService).record(eq(tenantId), eq(userId), eq("TENANT_LOGO_UPLOADED"), eq("TENANT"), eq(tenantId), any());
    }

    @Test
    @DisplayName("uploadLogo con archivo vacío lanza validación")
    void uploadLogoEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> tenantService.uploadLogo(tenantId, file))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
