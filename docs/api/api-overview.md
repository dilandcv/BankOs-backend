# Visión general de la API

## Estado

- **Implementado:** API REST JSON `/api/v1`, seguridad JWT, RBAC, multitenancy, validación, manejo de errores, paginación y OpenAPI/Swagger con springdoc.
- **Alcance:** backend independiente para curl, tests y clientes HTTP. `mobile/` no fue modificado ni conectado.

## Principios

- **Estilo:** REST sobre HTTP, JSON (`Content-Type: application/json`).
- **Versionado:** prefijo `/api/v1`.
- **Seguridad:** `Authorization: Bearer <JWT>`; roles y tenant desde el token.
- **Trazabilidad:** `X-Correlation-ID` opcional en solicitud; siempre se devuelve `correlation_id`. Si no se envía, el servidor genera un UUID.
- **Idempotencia:** `Idempotency-Key` obligatorio en depósitos, retiros y transferencias.
- **Consistencia:** sobre `success/code/message/data/correlation_id`, JSON `snake_case`, `BigDecimal` serializado como string.
- **Validación:** Jakarta Bean Validation con errores por campo.

## Encabezados

```http
Authorization: Bearer <JWT>
Content-Type: application/json
X-Correlation-ID: <UUID>        # opcional
Idempotency-Key: <UNIQUE_KEY>   # obligatorio en operaciones financieras
```

## Formato de respuesta

### Éxito

```json
{
  "success": true,
  "code": "DEPOSIT_COMPLETED",
  "message": "Depósito realizado correctamente",
  "data": { },
  "correlation_id": "uuid"
}
```

### Error

```json
{
  "success": false,
  "code": "INSUFFICIENT_FUNDS",
  "message": "Saldo insuficiente",
  "data": null,
  "correlation_id": "uuid"
}
```

### Error de validación

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Datos de entrada inválidos",
  "data": {
    "amount": "El monto debe ser mayor o igual a 0.0001"
  },
  "correlation_id": "uuid"
}
```

## Códigos HTTP

| HTTP | Uso |
|---|---|
| 200 | Operación exitosa o consulta. |
| 400 | JSON inválido, header faltante o validación de entrada. |
| 401 | Token ausente, inválido o expirado. |
| 403 | Rol insuficiente. |
| 404 | Recurso no encontrado en el tenant. |
| 409 | Duplicado, conflicto de estado o idempotencia con cuerpo distinto. |
| 422 | Regla de negocio no satisfecha (saldo, cuenta inactiva, tasa faltante). |
| 500 | Error interno. |

## Paginación

Listados usan Spring Data `Pageable`:

```http
GET /api/v1/transactions?page=0&size=20&accountId=...&type=DEPOSIT&from=2026-01-01T00:00:00Z
```

Respuesta:

```json
{
  "success": true,
  "code": "TRANSACTIONS_LISTED",
  "message": "Historial de transacciones",
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "total_elements": 0,
    "total_pages": 0
  },
  "correlation_id": "uuid"
}
```

## Endpoints implementados

| Método | Endpoint | Funcionalidad | Roles |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Login por institución | Público |
| `GET` | `/api/v1/auth/me` | Usuario/tenant del token | Autenticado |
| `GET/POST` | `/api/v1/tenants` | Listar/crear instituciones | `PLATFORM_ADMIN` (crear), admin lectura |
| `GET/POST` | `/api/v1/users` | Usuarios del tenant | `PLATFORM_ADMIN`, `TENANT_ADMIN` |
| `GET/POST` | `/api/v1/customers` | Clientes | Admin/Operador escritura, Auditor lectura |
| `GET/POST` | `/api/v1/accounts` | Cuentas | Admin/Operador escritura, Auditor lectura |
| `GET/PUT` | `/api/v1/settings` | Configuración | Admin escritura, lectura amplia |
| `GET/POST` | `/api/v1/settings/exchange-rates` | Tasas de cambio | Admin escritura, lectura amplia |
| `POST` | `/api/v1/transactions/deposits` | Depósito idempotente | Admin/Operador |
| `POST` | `/api/v1/transactions/withdrawals` | Retiro idempotente | Admin/Operador |
| `POST` | `/api/v1/transactions/transfers` | Transferencia idempotente | Admin/Operador |
| `POST` | `/api/v1/transactions/transfers/preview` | Tasa/comisión estimadas | Admin/Operador/Auditor |
| `GET` | `/api/v1/transactions` | Historial con filtros | Admin/Operador/Auditor |
| `GET` | `/api/v1/transactions/{id}` | Detalle de transacción | Admin/Operador/Auditor |
| `GET` | `/api/v1/audit-logs` | Auditoría del tenant | Admin/Auditor |
| `GET/POST` | `/api/v1/webhooks` | Configuración de webhooks | Admin escritura, Auditor lectura |
| `GET` | `/api/v1/webhooks/events` | Outbox de eventos | Admin/Auditor |

Ver contratos detallados en [api-contracts.md](api-contracts.md) y códigos en [error-handling.md](error-handling.md).

## OpenAPI / Swagger

- Swagger UI (perfil `dev`): `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- La documentación incluye el esquema Bearer.

## Independencia del frontend

- Los contratos permiten probar el backend con curl o clientes HTTP.
- **No** se debe modificar `mobile/` para consumir estos endpoints en esta fase.
- La integración multi-banco del cliente está documentada como pendiente en [Modelo multi-banco](../../../docs/architecture/multi-bank-model.md).
