# Backend BankOS — Documentación

> **Estado actual (2026-11-17):** backend MVP **implementado y verificado** en `backend/`. Incluye Spring Boot 3.5.0, Java 21, PostgreSQL/Flyway, Redis configurado, JWT, multitenancy, operaciones financieras idempotentes, auditoría y webhooks/outbox.
>
> **Alcance:** backend independiente y verificable por sí mismo. `mobile/` **no fue modificado ni conectado** en esta fase.
>
> **Modelo de producto:** [multi-banco con Opción A](../../docs/architecture/multi-bank-model.md): un login por institución y múltiples tokens almacenados en la app.

## Índice

### Planificación
- [Plan de implementación del backend](implementation-plan.md)

### Arquitectura
- [Visión general de arquitectura](architecture/architecture-overview.md)
- [Módulos](architecture/modules.md)
- [Diseño de base de datos](architecture/database-design.md)

### Desarrollo
- [Guía de inicio](development/getting-started.md)
- [Estándares de codificación](development/coding-standards.md)
- [Flujo de trabajo Git](development/git-workflow.md)

### API
- [Visión general de la API](api/api-overview.md)
- [Contratos de API](api/api-contracts.md)
- [Manejo de errores](api/error-handling.md)

### Seguridad
- [Autenticación](security/authentication.md)
- [Multitenancy](security/multitenancy.md)
- [Seguridad financiera](security/financial-security.md)

### Infraestructura
- [Configuración Docker](infrastructure/docker-setup.md)

### Calidad
- [Estrategia de pruebas](quality/testing-strategy.md)
- [Definition of Done](quality/definition-of-done.md)

## Resumen de implementación

| Componente | Estado |
|---|---|
| Proyecto Maven / Spring Boot 3.5.0 / Java 21 | Implementado |
| Estructura `common/*` + `modules/*` | Implementado |
| Spring Security / JWT HS256 / RBAC | Implementado |
| Multitenancy con `tenant_id` desde JWT | Implementado |
| PostgreSQL / Flyway (`V1`–`V6`) / seed dev `V100` | Implementado |
| Redis / Actuator | Configurado |
| Dockerfile / Docker Compose / healthchecks | Implementado |
| API REST `/api/v1` + OpenAPI/Swagger | Implementado (MVP) |
| Idempotencia, auditoría, webhooks/outbox | Implementado (MVP) |
| Pruebas unitarias + integración Testcontainers | Implementado (base, 3 + 4) |
| CI/CD | Pendiente |

## Endpoints MVP

- `POST /api/v1/auth/login` (público, por `tenant_code`)
- `GET /api/v1/auth/me`
- `GET/POST /api/v1/tenants` (crear solo `PLATFORM_ADMIN`)
- `GET/POST /api/v1/users`
- `GET/POST /api/v1/customers`
- `GET/POST /api/v1/accounts`
- `GET/PUT /api/v1/settings`
- `GET/POST /api/v1/settings/exchange-rates`
- `POST /api/v1/transactions/deposits`
- `POST /api/v1/transactions/withdrawals`
- `POST /api/v1/transactions/transfers`
- `POST /api/v1/transactions/transfers/preview`
- `GET /api/v1/transactions`
- `GET /api/v1/transactions/{id}`
- `GET /api/v1/audit-logs`
- `GET/POST /api/v1/webhooks`
- `GET /api/v1/webhooks/events`

## Decisiones clave implementadas

- Dinero: `NUMERIC(19,4)`, tasas `NUMERIC(19,8)`, `BigDecimal` escala 4, `HALF_UP`.
- Sin sobregiro en MVP: `CHECK (balance >= 0)` y validación de saldo.
- Tenant obligatorio en usuarios; `PLATFORM_ADMIN` en tenant reservado `PLATFORM`.
- Login `tenant_code + email + password`; JWT con `tenant_id` y `roles`.
- Aislamiento por filtros explícitos en repositorios/servicios, sin RLS en MVP.
- Idempotencia única por `(tenant_id, operation, idempotency_key)`, hash del request, snapshot de respuesta y advisory lock de PostgreSQL.
- Operaciones con bloqueo pesimista y orden determinista de cuentas en transferencias.
- Auditoría persistida y outbox de webhooks en la misma transacción.
- Envelope `success/code/message/data/correlation_id`, JSON `snake_case`, `BigDecimal` como string.

## Límites conocidos

- Sin refresh tokens ni revocación centralizada de sesiones.
- Sin onboarding público de instituciones.
- Sin RLS de PostgreSQL.
- La comisión de transferencia se descuenta del origen, sin cuenta de ingresos dedicada.
- El dispatcher de webhooks corre en la misma aplicación.
- El frontend sigue usando datos mock de un solo banco; la integración multi-banco está pendiente.
