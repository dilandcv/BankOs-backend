# Módulos del backend

## Estado

**Implementado:** todos los módulos del MVP existen en `com.bankos.modules` y `com.bankos.common`.

## Organización

Cada módulo puede contener:

```text
module/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
└── mapper/
```

No todos los módulos necesitan todas las capas. Las reglas de negocio viven en `service`, no en controllers.

## Módulos

| Módulo | Responsabilidad | Depende de |
|---|---|---|
| `auth` | Login por institución, JWT, `/auth/me`. | `tenants`, `users`, seguridad |
| `tenants` | Instituciones, branding y estado. | — |
| `users` | Usuarios por tenant y roles. | `tenants` |
| `customers` | Clientes del tenant. | `tenants` |
| `accounts` | Cuentas, saldos y bloqueos. | `tenants`, `customers` |
| `settings` | Configuración y tasas de cambio. | `tenants` |
| `idempotency` | Llaves, hash, snapshot y advisory lock. | — |
| `transactions` | Depósitos, retiros, transferencias, historial. | `accounts`, `settings`, `idempotency`, `audit` |
| `audit` | Registro de auditoría por tenant. | — |
| `webhooks` | Endpoints, outbox y dispatcher. | `audit` |

## Transversales (`common`)

| Paquete | Responsabilidad |
|---|---|
| `config` | Seguridad, OpenAPI, scheduling. |
| `security` | JWT, filtros, principal autenticado. |
| `tenancy` | `TenantContext`. |
| `exception` | `ApiException`, `ErrorCode`, handler global. |
| `response` | `ApiResponse`, `PageResponse`. |
| `money` | `MoneyUtils`. |
| `web` | Correlation ID. |

## Reglas de dependencia

- Evitar ciclos entre módulos.
- `transactions` puede depender de `accounts`, `settings`, `idempotency` y `audit`.
- `accounts` no depende de `transactions`.
- `webhooks` no revierte operaciones financieras.
- `common` no depende de módulos de dominio.

## Documentación relacionada

- [Arquitectura](architecture-overview.md)
- [Base de datos](database-design.md)
- [Seguridad financiera](../security/financial-security.md)
- [API](../api/api-overview.md)
