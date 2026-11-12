# Manejo de errores

## Estado
- **Implementado:** ninguno. No existe `@ControllerAdvice`, jerarquía de excepciones ni formato estándar en código.
- **Propuesto:** manejo global y consistente de errores con trazabilidad (`correlation_id`), códigos de negocio estables y mapeo HTTP claro.

## Formato estándar de error (propuesto)

```json
{
  "success": false,
  "code": "INSUFFICIENT_FUNDS",
  "message": "Saldo insuficiente",
  "data": null,
  "correlation_id": "uuid"
}
```

- `code`: identificador estable de negocio/técnico (para clientes y soporte).
- `message`: descripción legible y segura (sin filtrar detalles sensibles).
- `data`: `null` o detalle estructurado (p. ej. errores de validación por campo).
- `correlation_id`: mismo valor propagado desde `X-Correlation-ID` o generado por el servidor.

## Ejemplo de error de validación (propuesto)

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "La solicitud contiene datos inválidos",
  "data": {
    "fields": [
      { "field": "amount", "message": "debe ser mayor que 0" },
      { "field": "source_account_id", "message": "no debe ser nulo" }
    ]
  },
  "correlation_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Mapeo HTTP y códigos (propuesto)

| HTTP | Código (ejemplo) | Escenario |
|---|---|---|
| 400 | `VALIDATION_ERROR`, `BAD_REQUEST` | JSON inválido, campos faltantes/formato incorrecto |
| 401 | `UNAUTHORIZED`, `INVALID_TOKEN`, `TOKEN_EXPIRED` | Sin autenticación o JWT inválido/expirado |
| 403 | `FORBIDDEN`, `TENANT_ACCESS_DENIED` | Sin rol/permiso o recurso fuera del tenant |
| 404 | `RESOURCE_NOT_FOUND` | Recurso inexistente o no visible en el tenant |
| 409 | `INSUFFICIENT_FUNDS`, `ACCOUNT_INACTIVE`, `IDEMPOTENCY_CONFLICT`, `DUPLICATE_REQUEST_IN_PROGRESS` | Conflicto de negocio/estado/idempotencia |
| 422 | `BUSINESS_RULE_VIOLATION` | Regla de negocio no satisfecha (si se adopta) |
| 500 | `INTERNAL_ERROR` | Error no controlado (mensaje genérico) |

> Catálogo definitivo y mapeos exactos **pendientes de confirmación**.

## Errores de negocio financieros (propuestos)

| Código | Descripción | Nota |
|---|---|---|
| `INSUFFICIENT_FUNDS` | Saldo insuficiente para la operación | Validar en transacción atómica |
| `ACCOUNT_INACTIVE` / `ACCOUNT_BLOCKED` | Cuenta no operable | No permitir débito/crédito según política |
| `CURRENCY_MISMATCH` | Moneda inválida para la operación | Según reglas de cuenta/conversión |
| `EXCHANGE_RATE_NOT_FOUND` | No hay tasa vigente configurada | Configuración por tenant |
| `NEGATIVE_BALANCE_NOT_ALLOWED` | Operación dejaría saldo negativo | Salvo política de descubierto confirmada |
| `INVALID_TRANSFER` | Origen/destino inválidos (mismo recurso, estado, etc.) | Regla de negocio |

## Idempotencia (errores y respuestas, propuesto)

| Caso | Comportamiento esperado |
|---|---|
| Misma `Idempotency-Key` y mismo payload ya completado | Devolver el **mismo resultado** previamente registrado (código HTTP y cuerpo consistentes) |
| Misma `Idempotency-Key` y payload diferente | Rechazar con conflicto (p. ej. 409 `IDEMPOTENCY_CONFLICT`) |
| Solicitud simultánea con misma clave | Una procesa; las demás esperan o reciben estado "en progreso" según diseño (409/202 a definir) |
| Reintento tras fallo transitorio | Permitir reintento seguro según estado persistido en PostgreSQL |

> PostgreSQL es la fuente de verdad del registro de idempotencia; Redis solo complementario.

## Manejo global de excepciones (propuesto)

- `@ControllerAdvice` para traducir excepciones a respuestas estándar.
- Excepciones de dominio (p. ej. saldo insuficiente, recurso no encontrado, acceso denegado) con código y HTTP explícitos.
- Excepciones no controladas: registrar detalle internamente con `correlation_id` y responder `500 INTERNAL_ERROR` sin exponer stack traces.
- Validación Jakarta: agregar errores de campo en `data.fields`.

## Seguridad en mensajes de error
- No revelar si un recurso pertenece a otro tenant (preferir 404/403 según política, sin fuga de existencia).
- No exponer información sensible (hashes, secretos, consultas SQL, trazas).
- Mensajes claros pero genéricos en autenticación cuando corresponda (`401`/`403`).