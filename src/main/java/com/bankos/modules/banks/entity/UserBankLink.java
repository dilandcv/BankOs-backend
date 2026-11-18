package com.bankos.modules.banks.entity;

import com.bankos.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_bank_links")
public class UserBankLink extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "account_nickname", length = 100)
    private String accountNickname;

    @Column(name = "account_number_mask", length = 30)
    private String accountNumberMask;

    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType = "SAVINGS";

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_status", nullable = false, length = 30)
    private BankAuthStatus authStatus = BankAuthStatus.CONNECTED;

    @Column(name = "auth_mechanism", nullable = false, length = 50)
    private String authMechanism = "OAUTH2";

    @Column(name = "auth_token_encrypted", columnDefinition = "TEXT")
    private String authTokenEncrypted;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getAccountNickname() {
        return accountNickname;
    }

    public void setAccountNickname(String accountNickname) {
        this.accountNickname = accountNickname;
    }

    public String getAccountNumberMask() {
        return accountNumberMask;
    }

    public void setAccountNumberMask(String accountNumberMask) {
        this.accountNumberMask = accountNumberMask;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BankAuthStatus getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(BankAuthStatus authStatus) {
        this.authStatus = authStatus;
    }

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    public String getAuthTokenEncrypted() {
        return authTokenEncrypted;
    }

    public void setAuthTokenEncrypted(String authTokenEncrypted) {
        this.authTokenEncrypted = authTokenEncrypted;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
}
