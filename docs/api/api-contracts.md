# Contratos de API (propuestos)

## Estado y alcance
- **Implementado:** ninguno (sin controladores ni endpoints en el repositorio).
- **Propuesto:** contratos REST JSON bajo `/api/v1` para desarrollo y prueba independiente del backend.
- **Nota:** estos contratos son **propuestas** hasta validarse con requisitos y la implementación real. No implican integración con `mobile/`.

## Convenciones comunes

### Encabezados
```http
Authorization: Bearer <JWT>        # requerido en endpoints protegidos
Content-Type: application/json
X-Correlation-ID: <UUID>          # trazabilidad
Idempotency-Key: <UNIQUE_KEY>     # requerido en operaciones financieras
```

### Envoltorio de respuesta (propuesto)
```json
{
  "success": true,
  "code": "OPERATION_SUCCESS",
  "message": "Operación realizada correctamente",
  "data": { },
  "correlation_id": "uuid"
}
```

## Autenticación y tenant

| Método | Endpoint | Descripción | Auth | Estado |
|---|---|---|---|---|
| POST | `/tenants` | Registrar institución (tenant) | Público/administrativo (definir) | Propuesto |
| POST | `/auth/login` | Iniciar sesión y emitir JWT | Público | Propuesto |
| GET | `/auth/me` | Datos del usuario autenticado y contexto | Bearer JWT | Propuesto |

### Ejemplo: login (propuesto)
**Request**
```json
{
  "tenant": "tenant-code",
  "username": "user@example.com",
  "password": "********"
}
```
**Response `data` (ejemplo)**
```json
{
  "access_token": "<jwt>",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

> El mecanismo exacto de identificación de tenant en login (campo, subdominio, header) debe confirmarse; el `tenant` **nunca** debe confiarse en operaciones posteriores sin validarlo contra el contexto del JWT.

## Cuentas

| Método | Endpoint | Descripción | Estado |
|---|---|---|---|
| GET | `/accounts` | Listar cuentas del tenant (paginado) | Propuesto |
| GET | `/accounts/{id}` | Consultar cuenta por id (del tenant) | Propuesto |
| POST | `/accounts` | Crear cuenta (admin) | Propuesto |
| PATCH | `/accounts/{id}` | Actualizar estado/datos permitidos (admin) | Propuesto |

**Response `data` cuenta (ejemplo)**
```json
{
  "id": "uuid",
  "customer_id": "uuid",
  "account_number": "000123",
  "currency": "USD",
  "balance": "1000.00",
  "status": "ACTIVE"
}
```

## Transacciones

| Método | Endpoint | Descripción | Idempotency-Key | Estado |
|---|---|---|---|---|
| POST | `/transactions/deposits` | Depósito en cuenta | Requerido | Propuesto |
| POST | `/transactions/withdrawals` | Retiro de cuenta | Requerido | Propuesto |
| POST | `/transactions/transfers/preview` | Previsualizar comisión/conversión/montos | Recomendado/definir | Propuesto |
| POST | `/transactions/transfers` | Ejecutar transferencia | Requerido | Propuesto |
| GET | `/transactions` | Consultar movimientos (paginado/filtrado) | No | Propuesto |
| GET | `/transactions/{id}` | Consultar transacción | No | Propuesto |

### Ejemplo: depósito (propuesto)
```json
{
  "account_id": "uuid",
  "amount": "150.00",
  "currency": "USD",
  "reference": "cash-in"
}
```

### Ejemplo: retiro (propuesto)
```json
{
  "account_id": "uuid",
  "amount": "50.00",
  "currency": "USD",
  "reference": "atm"
}
```

### Ejemplo: preview de transferencia (propuesto)
```json
{
  "source_account_id": "uuid",
  "destination_account_id": "uuid",
  "amount": "100.00",
  "currency": "USD"
}
```
**Response `data` (ejemplo)**
```json
{
  "amount": "100.00",
  "fee": "2.00",
  "fee_currency": "USD",
  "exchange_rate": "1.0000",
  "credited_amount": "100.00",
  "credited_currency": "USD",
  "total_debit": "102.00"
}
```

### Ejemplo: transferencia (propuesto)
```json
{
  "source_account_id": "uuid",
  "destination_account_id": "uuid",
  "amount": "100.00",
  "currency": "USD",
  "reference": "invoice-123"
}
```

**Reglas propuestas:** comisión se descuenta de la cuenta origen; la conversión se aplica al monto transferido (no a la comisión); tasas configuradas por tenant. Validar pertenencia al tenant, estados, saldo y ejecutar de forma atómica.

## Recursos administrativos (propuestos)

| Recurso | Endpoints (ejemplos) | Descripción | Estado |
|---|---|---|---|
| Usuarios | `GET/POST /users`, `GET/PATCH /users/{id}` | Gestión de usuarios, roles y permisos | Propuesto |
| Clientes | `GET/POST /customers`, `GET/PATCH /customers/{id}` | Gestión de clientes | Propuesto |
| Cuentas (admin) | `POST/PATCH /accounts` | Alta y cambios de estado | Propuesto |
| Configuraciones | `GET/PUT /settings` o por claves | Parámetros y comisiones por tenant | Propuesto |
| Tasas de cambio | `GET/POST /exchange-rates` | Gestión de tasas por tenant | Propuesto |
| Auditoría | `GET /audit-logs` | Consulta de trazabilidad (solo lectura) | Propuesto |
| Webhooks | `GET/POST/PATCH /webhooks`, entregas/reintentos | Configuración y observabilidad de entregas | Propuesto |

> Rutas y permisos exactos (RBAC) deben definirse al implementar.

## Paginación (propuesta)
Parámetros sugeridos: `page`, `size`, `sort`, `from`, `to`, `status`.
```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "total_items": 0,
  "total_pages": 0
}
```

## Validaciones y errores (propuestos)
- 400: errores de formato/validación (`data` puede incluir detalle por campo).
- 401/403: autenticación/autorización/tenant inválido.
- 404: recurso inexistente o no perteneciente al tenant.
- 409: conflicto de estado, duplicado o idempotencia (misma clave, payload distinto o en proceso, según diseño).
- Ver [Manejo de errores](error-handling.md) para catálogo y ejemplos.