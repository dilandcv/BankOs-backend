# Contratos de API

> **Estado:** contratos implementados del MVP. Cualquier cambio requiere actualizar controladores, DTOs y pruebas.

## Convenciones

### Encabezados

```http
Authorization: Bearer <JWT>        # endpoints protegidos
Content-Type: application/json
X-Correlation-ID: <UUID>          # opcional; se genera si falta
Idempotency-Key: <UNIQUE_KEY>     # obligatorio en operaciones financieras
```

### Sobre de respuesta

```json
{
  "success": true,
  "code": "OPERATION_SUCCESS",
  "message": "Operación realizada correctamente",
  "data": { },
  "correlation_id": "uuid"
}
```

- JSON en `snake_case` para cuerpos y respuestas de DTO.
- Excepción: en `400 VALIDATION_ERROR`, las claves de `data` usan el nombre del campo Java (`camelCase`, p. ej. `accountId`, `sourceAccountId`, `tenantCode`), porque `GlobalExceptionHandler` toma `FieldError.getField()`. Los clientes deben leer esas claves tal cual.
- `BigDecimal` se serializa como string (`"100.0000"`).
- Fechas en ISO-8601 UTC (`Instant`).
- Roles: `PLATFORM_ADMIN`, `TENANT_ADMIN`, `OPERATOR`, `AUDITOR`. En las tablas, “Admin” = `PLATFORM_ADMIN` + `TENANT_ADMIN`; “Admin/Operador/Auditor” = los cuatro roles.

## Autenticación y tenant

| Método | Endpoint | Descripción | Auth |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Login por institución | Público |
| `GET` | `/api/v1/auth/me` | Usuario y tenant del token | Bearer |
| `POST` | `/api/v1/tenants` | Crear institución | `PLATFORM_ADMIN` |
| `GET` | `/api/v1/tenants` | Listar instituciones accesibles | `PLATFORM_ADMIN`, `TENANT_ADMIN` |
| `GET` | `/api/v1/tenants/{id}` | Consultar institución | `PLATFORM_ADMIN`, `TENANT_ADMIN` |

### Login

**Request**

```json
{
  "tenant_code": "BANCO_HORIZONTE",
  "email": "admin@horizonte.com",
  "password": "Admin123!"
}
```

**Response `data`**

```json
{
  "access_token": "<jwt>",
  "token_type": "Bearer",
  "expires_in": 3600,
  "user": {
    "id": "uuid",
    "email": "admin@horizonte.com",
    "full_name": "Admin Banco Horizonte",
    "role": "TENANT_ADMIN"
  },
  "tenant": {
    "id": "uuid",
    "code": "BANCO_HORIZONTE",
    "name": "Banco Horizonte",
    "primary_color": "#0070BA",
    "secondary_color": "#003087",
    "logo_url": null
  }
}
```

### Crear institución

**Request**

```json
{
  "code": "BANCO_NUEVO",
  "name": "Banco Nuevo",
  "primary_color": "#112233",
  "secondary_color": "#445566",
  "logo_url": "https://cdn.example.com/logo.png"
}
```

**Response `data`**

```json
{
  "id": "uuid",
  "code": "BANCO_NUEVO",
  "name": "Banco Nuevo",
  "status": "ACTIVE",
  "primary_color": "#112233",
  "secondary_color": "#445566",
  "logo_url": "https://cdn.example.com/logo.png",
  "created_at": "2026-11-16T12:00:00Z"
}
```

## Usuarios

| Método | Endpoint | Descripción | Roles |
|---|---|---|---|
| `GET` | `/api/v1/users` | Listar usuarios del tenant | `PLATFORM_ADMIN`, `TENANT_ADMIN` |
| `POST` | `/api/v1/users` | Crear usuario | `PLATFORM_ADMIN`, `TENANT_ADMIN` |
| `GET` | `/api/v1/users/{id}` | Consultar usuario | `PLATFORM_ADMIN`, `TENANT_ADMIN` |

**Request crear**

```json
{
  "email": "operador@banco.com",
  "full_name": "Operador Banco",
  "password": "Password123!",
  "role": "OPERATOR"
}
```

**Response `data`**

```json
{
  "id": "uuid",
  "email": "operador@banco.com",
  "full_name": "Operador Banco",
  "role": "OPERATOR",
  "status": "ACTIVE",
  "created_at": "2026-11-16T12:00:00Z"
}
```

## Clientes

| Método | Endpoint | Descripción | Roles |
|---|---|---|---|
| `GET` | `/api/v1/customers` | Listar clientes | Admin/Operador/Auditor |
| `POST` | `/api/v1/customers` | Crear cliente | `TENANT_ADMIN`, `OPERATOR` |
| `GET` | `/api/v1/customers/{id}` | Consultar cliente | Admin/Operador/Auditor |

**Request**

```json
{
  "document_type": "CC",
  "document_number": "1020304050",
  "full_name": "María González"
}
```

**Response `data`**

```json
{
  "id": "uuid",
  "document_type": "CC",
  "document_number": "1020304050",
  "full_name": "María González",
  "status": "ACTIVE",
  "created_at": "2026-11-16T12:00:00Z"
}
```

## Cuentas

| Método | Endpoint | Descripción | Roles |
|---|---|---|---|
| `GET` | `/api/v1/accounts` | Listar cuentas paginadas | Admin/Operador/Auditor |
| `GET` | `/api/v1/accounts/all` | Listar todas las cuentas | Admin/Operador/Auditor |
| `POST` | `/api/v1/accounts` | Crear cuenta | `TENANT_ADMIN`, `OPERATOR` |
| `GET` | `/api/v1/accounts/{id}` | Consultar cuenta | Admin/Operador/Auditor |

**Request crear**

```json
{
  "customer_id": "uuid",
  "currency": "USD"
}
```

**Response `data`**

```json
{
  "id": "uuid",
  "customer_id": "uuid",
  "account_number": "0000004821",
  "currency": "USD",
  "balance": "8250.0000",
  "status": "ACTIVE",
  "created_at": "2026-11-16T12:00:00Z"
}
```

## Transacciones

| Método | Endpoint | Descripción | Idempotency-Key | Roles |
|---|---|---|---|---|
| `POST` | `/api/v1/transactions/deposits` | Depósito | Requerido | Admin/Operador |
| `POST` | `/api/v1/transactions/withdrawals` | Retiro | Requerido | Admin/Operador |
| `POST` | `/api/v1/transactions/transfers/preview` | Comisión/conversión | No | Admin/Operador/Auditor |
| `POST` | `/api/v1/transactions/transfers` | Transferencia | Requerido | Admin/Operador |
| `GET` | `/api/v1/transactions` | Historial paginado | No | Admin/Operador/Auditor |
| `GET` | `/api/v1/transactions/{id}` | Detalle | No | Admin/Operador/Auditor |

### Depósito

```json
{
  "account_id": "uuid",
  "amount": "100.00",
  "currency": "USD",
  "description": "Depósito demo"
}
```

### Retiro

```json
{
  "account_id": "uuid",
  "amount": "50.00",
  "currency": "USD",
  "description": "Retiro ATM"
}
```

### Preview de transferencia

**Request**

```json
{
  "source_account_id": "uuid",
  "target_account_id": "uuid",
  "amount": "100.00",
  "currency": "USD"
}
```

**Response `data`**

```json
{
  "source_account_id": "uuid",
  "target_account_id": "uuid",
  "amount": "100.0000",
  "source_currency": "USD",
  "target_currency": "EUR",
  "exchange_rate": "0.92000000",
  "fee": "1.5000",
  "debit_amount": "101.5000",
  "credit_amount": "92.0000"
}
```

### Transferencia

```json
{
  "source_account_id": "uuid",
  "target_account_id": "uuid",
  "amount": "100.00",
  "currency": "USD",
  "description": "Pago factura"
}
```

**Response `data` de operación financiera**

```json
{
  "id": "uuid",
  "type": "TRANSFER",
  "status": "COMPLETED",
  "amount": "100.0000",
  "currency": "USD",
  "fee": "1.5000",
  "exchange_rate": "0.92000000",
  "source_account_id": "uuid",
  "destination_account_id": "uuid",
  "description": "Pago factura",
  "correlation_id": "uuid",
  "created_at": "2026-11-16T12:00:00Z",
  "entries": [
    {
      "id": "uuid",
      "account_id": "uuid",
      "direction": "DEBIT",
      "amount": "101.5000",
      "currency": "USD",
      "created_at": "2026-11-16T12:00:00Z"
    },
    {
      "id": "uuid",
      "account_id": "uuid",
      "direction": "CREDIT",
      "amount": "92.0000",
      "currency": "EUR",
      "created_at": "2026-11-16T12:00:00Z"
    }
  ]
}
```

**Reglas implementadas**

- El `tenant_id` de origen y destino se toma del JWT; no se acepta en el body.
- La comisión se descuenta de la cuenta origen.
- La conversión se aplica al monto transferido, no a la comisión.
- La tasa se resuelve por tenant; si no existe, `EXCHANGE_RATE_NOT_FOUND`.
- Saldo insuficiente, cuenta inactiva/bloqueada y moneda incorrecta se rechazan.
- Bloqueo pesimista con orden determinista de cuentas.
- La misma `Idempotency-Key` + mismo body devuelve la respuesta original; body distinto devuelve `409 IDEMPOTENCY_CONFLICT`.

### Historial

```http
GET /api/v1/transactions?page=0&size=20&accountId=uuid&type=DEPOSIT&from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z
```

## Configuración y tasas

| Método | Endpoint | Descripción | Roles |
|---|---|---|---|
| `GET` | `/api/v1/settings` | Configuración del tenant | Admin/Operador/Auditor |
| `PUT` | `/api/v1/settings` | Actualizar claves | Admin |
| `GET` | `/api/v1/settings/exchange-rates` | Listar tasas | Admin/Operador/Auditor |
| `GET` | `/api/v1/settings/exchange-rates/{id}` | Consultar tasa | Admin/Operador/Auditor |
| `POST` | `/api/v1/settings/exchange-rates` | Crear tasa | Admin |

**Request settings**

```json
{
  "settings": {
    "transfer.fee.fixed": "1.5000",
    "transfer.fee.percent": "0.5000"
  }
}
```

`transfer.fee.percent` se interpreta como porcentaje (ej. `0.5` = 0.5%).

**Request tasa**

```json
{
  "from_currency": "USD",
  "to_currency": "EUR",
  "rate": "0.92000000",
  "effective_from": "2026-01-01T00:00:00Z",
  "effective_to": null
}
```

## Auditoría

| Método | Endpoint | Descripción | Roles |
|---|---|---|---|
| `GET` | `/api/v1/audit-logs` | Listar auditoría del tenant | Admin/Auditor |
| `GET` | `/api/v1/audit-logs/{id}` | Detalle | Admin/Auditor |

**Response `data`**

```json
{
  "id": "uuid",
  "actor_user_id": "uuid",
  "action": "TRANSACTION_DEPOSIT",
  "resource_type": "TRANSACTION",
  "resource_id": "uuid",
  "metadata": "{\"amount\":\"100.0000\"}",
  "correlation_id": "uuid",
  "created_at": "2026-11-16T12:00:00Z"
}
```

## Webhooks

| Método | Endpoint | Descripción | Roles |
|---|---|---|---|
| `GET` | `/api/v1/webhooks` | Listar configuraciones | Admin/Auditor |
| `POST` | `/api/v1/webhooks` | Crear webhook | Admin |
| `GET` | `/api/v1/webhooks/events` | Listar outbox (paginado) | Admin/Auditor |

**Request crear webhook**

```json
{
  "url": "https://example.com/bankos/events",
  "events": "transaction.completed",
  "secret": "super-secret-at-least-16"
}
```

- El secreto no se devuelve en respuestas.
- `GET /webhooks` devuelve un array; `GET /webhooks/events` devuelve una [respuesta paginada](#paginación).
- Entrega con `X-BankOS-Event` y `X-BankOS-Signature: sha256=<hmac>`.
- Reintentos exponenciales; estado final `DEAD`.

## Paginación

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "total_elements": 0,
  "total_pages": 0
}
```

## Errores

- `400 VALIDATION_ERROR`: validación de campos o header faltante. `data` es un mapa `{campoJava: mensaje}` con claves camelCase, p. ej. `{"accountId": "must not be null"}`.
- `401 UNAUTHORIZED`: token ausente/inválido/expirado.
- `403 FORBIDDEN`: rol insuficiente.
- `404 *_NOT_FOUND`: recurso inexistente o de otro tenant. Excepción: `GET /api/v1/settings/exchange-rates/{id}` responde `409 CONFLICT` con mensaje «Tasa de cambio no encontrada» (`SettingsService.getExchangeRate`).
- `409 CONFLICT` / `IDEMPOTENCY_CONFLICT`: duplicado o cuerpo distinto con la misma llave.
- `422` reglas de negocio (`INSUFFICIENT_FUNDS`, `CURRENCY_MISMATCH`, `ACCOUNT_INACTIVE`, `EXCHANGE_RATE_NOT_FOUND`).
- `500 INTERNAL_ERROR`.

> **Discrepancia conocida (2026-11-17):** una denegación por `@PreAuthorize` lanza `AuthorizationDeniedException` (Spring Security 6.5), que el `@ExceptionHandler(Exception.class)` de `GlobalExceptionHandler` convierte en `500 INTERNAL_ERROR` en lugar de `403 FORBIDDEN`. Reproducido con `POST /tenants` usando un `TENANT_ADMIN`. El cliente móvil previene estas llamadas con guards de rol; el comportamiento del backend no se modificó en el plan de integración móvil.

Ver [Manejo de errores](error-handling.md).
