package com.bankos.modules.webhooks.service;

import com.bankos.modules.webhooks.entity.OutboxEvent;
import com.bankos.modules.webhooks.entity.OutboxStatus;
import com.bankos.modules.webhooks.entity.WebhookConfig;
import com.bankos.modules.webhooks.entity.WebhookStatus;
import com.bankos.modules.webhooks.repository.OutboxEventRepository;
import com.bankos.modules.webhooks.repository.WebhookConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final WebhookConfigRepository webhookConfigRepository;
    private final RestClient webhookRestClient;
    private final int maxAttempts;

    public OutboxDispatcher(
            OutboxEventRepository outboxEventRepository,
            WebhookConfigRepository webhookConfigRepository,
            RestClient webhookRestClient,
            @Value("${app.webhooks.max-attempts:8}") int maxAttempts
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.webhookConfigRepository = webhookConfigRepository;
        this.webhookRestClient = webhookRestClient;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${app.webhooks.dispatch-interval-ms:5000}")
    @Transactional
    public void dispatch() {
        List<OutboxEvent> events = outboxEventRepository.lockPending(20);
        for (OutboxEvent event : events) {
            process(event);
        }
    }

    private void process(OutboxEvent event) {
        Instant now = Instant.now();
        WebhookConfig config = event.getWebhookConfigId() == null
                ? null
                : webhookConfigRepository.findById(event.getWebhookConfigId()).orElse(null);

        if (config == null || config.getStatus() != WebhookStatus.ACTIVE) {
            event.setStatus(OutboxStatus.DEAD);
            event.setProcessedAt(now);
            outboxEventRepository.save(event);
            return;
        }

        try {
            String signature = sign(config.getSecret(), event.getPayload());
            webhookRestClient.post()
                    .uri(config.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-BankOS-Event", event.getEventType())
                    .header("X-BankOS-Signature", "sha256=" + signature)
                    .body(event.getPayload())
                    .retrieve()
                    .toBodilessEntity();

            event.setAttempts(event.getAttempts() + 1);
            event.setStatus(OutboxStatus.DELIVERED);
            event.setProcessedAt(now);
        } catch (Exception ex) {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            if (attempts >= maxAttempts) {
                event.setStatus(OutboxStatus.DEAD);
                event.setProcessedAt(now);
            } else {
                long backoffSeconds = Math.min(3600L, (long) Math.pow(2, attempts) * 5L);
                event.setNextRetryAt(now.plusSeconds(backoffSeconds));
            }
            log.warn("Webhook {} falló (intento {}): {}", event.getId(), attempts, ex.getMessage());
        }
        outboxEventRepository.save(event);
    }

    private String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el webhook", ex);
        }
    }
}
