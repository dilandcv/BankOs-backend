# Manejo de errores

## Estado

- **Implementado:** `GlobalExceptionHandler` (`@RestControllerAdvice`), `ApiException`, `ErrorCode` y sobre de error con `correlation_id`.
- **Implementado:** validación Jakarta por campo, header faltante, JSON inválido y violaciones de integridad.
- **Pendiente:** catálogo de errores de infraestructura (timeouts, webhooks) y traducción de mensajes.

## Formato estándar

```json
{
  "success": false,
  "code": "INSUFFICIENT_FUNDS",
  "message": "Saldo insuficiente",
  "data": null,
  "correlation_id": "uuid"
}
```

- `code`: identificador estable (`ErrorCode`).
- `message`: mensaje seguro para el cliente.
- `data`: `null` o detalle estructurado.
- `correlation_id`: el mismo de `X-Correlation-ID` o uno generado por el servidor.

## Error de validación

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Datos de entrada inválidos",
  "data": {
    "amount": "El monto debe ser mayor o igual a 0.0001",
    "currency": "La moneda debe tener 3 letras"
  },
  "correlation_id": "uuid"
}
```

## Catálogo implementado

| HTTP | Código | Escenario |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Campos inválidos, JSON ilegible o header incorrecto. |
| 400 | `IDEMPOTENCY_KEY_REQUIRED` | Falta `Idempotency-Key` en operación financiera. |
| 401 | `INVALID_CREDENTIALS` | Login con tenant/usuario/contraseña inválidos. |
| 401 | `UNAUTHORIZED` | Token ausente, inválido o expirado. |
| 403 | `FORBIDDEN` | Rol insuficiente. |
| 403 | `TENANT_ACCESS_DENIED` | Acceso a recursos de otra institución. |
| 404 | `TENANT_NOT_FOUND` | Institución inexistente. |
| 404 | `USER_NOT_FOUND` | Usuario inexistente en el tenant. |
| 404 | `CUSTOMER_NOT_FOUND` | Cliente inexistente en el tenant. |
| 404 | `ACCOUNT_NOT_FOUND` | Cuenta inexistente en el tenant. |
| 404 | `TRANSACTION_NOT_FOUND` | Transacción inexistente en el tenant. |
| 404 | `AUDIT_LOG_NOT_FOUND` | Registro de auditoría inexistente. |
| 409 | `CONFLICT` | Duplicado o estado incompatible. |
| 409 | `IDEMPOTENCY_CONFLICT` | Misma llave con cuerpo distinto. |
| 409 | `IDEMPOTENCY_IN_PROGRESS` | Operación concurrente con la misma llave. |
| 422 | `INSUFFICIENT_FUNDS` | Saldo insuficiente. |
| 422 | `ACCOUNT_INACTIVE` | Cuenta no activa. |
| 422 | `ACCOUNT_BLOCKED` | Cuenta bloqueada. |
| 422 | `SAME_ACCOUNT_TRANSFER` | Origen y destino iguales. |
| 422 | `CURRENCY_NOT_SUPPORTED` | Moneda con formato inválido. |
| 422 | `CURRENCY_MISMATCH` | Moneda distinta a la cuenta. |
| 422 | `EXCHANGE_RATE_NOT_FOUND` | No hay tasa configurada. |
| 422 | `INVALID_AMOUNT` | Monto no positivo o conversión demasiado pequeña. |
| 429 | `RATE_LIMITED` | Reservado; rate limiting no implementado. |
| 500 | `INTERNAL_ERROR` | Error no controlado; mensaje genérico. |

## Idempotencia

| Caso | Comportamiento |
|---|---|
| Misma llave + mismo body, ya completado | Devuelve el mismo `ApiResponse` y `data` originales. |
| Misma llave + body distinto | `409 IDEMPOTENCY_CONFLICT`. |
| Solicitud concurrente con la misma llave | Advisory lock de PostgreSQL; la segunda espera o recibe `409 IDEMPOTENCY_IN_PROGRESS`. |
| Fallo transitorio | La transacción revierte; el registro `IN_PROGRESS` no queda completado si falla la transacción. |

El hash del request y el snapshot de respuesta se guardan en PostgreSQL (`idempotency_records`). Redis no se usa como fuente de verdad.

## Manejo global

- `ApiException` → `ErrorCode.status()` y `ApiResponse.error`.
- `MethodArgumentNotValidException` / `ConstraintViolationException` → `400 VALIDATION_ERROR` con errores por campo.
- `MissingRequestHeaderException` → `400 IDEMPOTENCY_KEY_REQUIRED` para operaciones financieras.
- `HttpMessageNotReadableException` → `400 VALIDATION_ERROR`.
- `DataIntegrityViolationException` → `409 CONFLICT` (sin exponer SQL).
- `Exception` → log interno con stack trace y `500 INTERNAL_ERROR` genérico.

## Seguridad en mensajes

- No se exponen hashes, secretos, SQL ni stack traces.
- Los recursos de otro tenant responden `404` para no revelar existencia.
- El login usa un mensaje genérico para no facilitar enumeración de tenants/usuarios.
