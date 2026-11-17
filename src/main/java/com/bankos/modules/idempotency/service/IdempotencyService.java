package com.bankos.modules.idempotency.service;

import com.bankos.common.exception.ApiException;
import com.bankos.common.exception.ErrorCode;
import com.bankos.modules.idempotency.entity.IdempotencyRecord;
import com.bankos.modules.idempotency.entity.IdempotencyStatus;
import com.bankos.modules.idempotency.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public IdempotencyService(
            IdempotencyRecordRepository idempotencyRecordRepository,
            ObjectMapper objectMapper,
            EntityManager entityManager
    ) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Start start(UUID tenantId, String operation, String idempotencyKey, String requestHash) {
        String lockKey = tenantId + ":" + operation + ":" + idempotencyKey;
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtext(:lockKey)::bigint)")
                .setParameter("lockKey", lockKey)
                .getSingleResult();

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository
                .findByTenantIdAndOperationAndIdempotencyKey(tenantId, operation, idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                return new Start(null, record.getResponsePayload());
            }
            if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_IN_PROGRESS);
            }
            throw new ApiException(ErrorCode.CONFLICT, "La operación idempotente terminó en estado fallido");
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setTenantId(tenantId);
        record.setOperation(operation);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyStatus.IN_PROGRESS);
        idempotencyRecordRepository.save(record);
        return new Start(record, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(IdempotencyRecord record, Object response) {
        record.setResponsePayload(toJson(response));
        record.setStatus(IdempotencyStatus.COMPLETED);
        idempotencyRecordRepository.save(record);
    }

    public String hash(Object request) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "No se pudo calcular la huella de idempotencia");
        } catch (NoSuchAlgorithmException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "SHA-256 no está disponible");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "No se pudo serializar la respuesta idempotente");
        }
    }

    public record Start(IdempotencyRecord record, String replayPayload) {
    }
}
