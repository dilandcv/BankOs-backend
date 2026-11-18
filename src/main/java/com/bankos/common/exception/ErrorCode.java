package com.bankos.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos"),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "El header Idempotency-Key es obligatorio"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Autenticación requerida"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta operación"),
    TENANT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "No puedes acceder a recursos de otra institución"),
    TENANT_INACTIVE(HttpStatus.FORBIDDEN, "La institución no está activa"),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "El usuario no está activo"),
    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "Institución no encontrada"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Usuario no encontrado"),
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "Cliente no encontrado"),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Cuenta no encontrada"),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Transacción no encontrada"),
    AUDIT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "Registro de auditoría no encontrado"),
    WEBHOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "Configuración de webhook no encontrada"),
    BANK_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "Cuenta bancaria vinculada no encontrada"),
    BANK_ALREADY_LINKED(HttpStatus.CONFLICT, "Esta institución bancaria ya está vinculada a tu cuenta"),
    BANK_REAUTH_REQUIRED(HttpStatus.FORBIDDEN, "La cuenta bancaria requiere reautorización"),
    BANK_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "Institución bancaria no soportada"),
    CONFLICT(HttpStatus.CONFLICT, "Conflicto con el estado actual del recurso"),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "La llave de idempotencia ya fue usada con un contenido diferente"),
    IDEMPOTENCY_IN_PROGRESS(HttpStatus.CONFLICT, "La operación con esta llave de idempotencia está en progreso"),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente"),
    ACCOUNT_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "La cuenta no está activa"),
    ACCOUNT_BLOCKED(HttpStatus.UNPROCESSABLE_ENTITY, "La cuenta está bloqueada"),
    SAME_ACCOUNT_TRANSFER(HttpStatus.UNPROCESSABLE_ENTITY, "La cuenta origen y destino no pueden ser la misma"),
    CURRENCY_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_ENTITY, "Moneda no soportada"),
    CURRENCY_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "La moneda no coincide con la cuenta"),
    EXCHANGE_RATE_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "No hay tasa de cambio configurada"),
    INVALID_AMOUNT(HttpStatus.UNPROCESSABLE_ENTITY, "El monto no es válido"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
