package com.bankos.modules.webhooks.entity;

import com.bankos.common.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "webhook_configs")
public class WebhookConfig extends TenantScopedEntity {

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 500)
    private String events;

    @Column(nullable = false, length = 255)
    private String secret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebhookStatus status = WebhookStatus.ACTIVE;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public WebhookStatus getStatus() {
        return status;
    }

    public void setStatus(WebhookStatus status) {
        this.status = status;
    }
}
