# BankOS Backend

Backend multi-banco y multi-tenant de BankOS. Implementa el modelo de **una sesión por institución**: cada usuario pertenece a un tenant, inicia sesión con `tenant_code + email + password`, y el cliente puede guardar varios tokens (uno por banco) para cambiar de institución sin volver a autenticarse.

> `mobile/` no fue modificado. Este backend es independiente y se puede ejecutar y probar solo con Docker.

## Requisitos

- Docker y Docker Compose.
- Opcional: Java 21 + Maven 3.9 si se quiere ejecutar sin Docker.

## Ejecución rápida

```bash
cd backend
cp .env.example .env
docker compose up --build
```

> Si el puerto `5432` ya está ocupado por otro PostgreSQL local, usa `DB_PORT=55432 docker compose up --build`. El backend siempre se conecta al PostgreSQL interno del stack en `5432`.

Servicios:

- API: http://localhost:8080
- Swagger UI (perfil `dev`): http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

El perfil `dev` carga `V100__dev_seed.sql` con las instituciones demo, usuarios, clientes, cuentas, configuración y tasas de cambio.

## Credenciales demo

Contraseña para todos los usuarios demo: `Admin123!`

| Institución | Código | Email |
|---|---|---|
| BankOS Platform | `PLATFORM` | `platform@bankos.com` |
| Nequi | `NEQUI` | `admin@nequi.com` |
| Daviplata | `DAVIPLATA` | `admin@daviplata.com` |
| PayPal | `PAYPAL` | `admin@paypal.com` |
| Bancolombia | `BANCOLOMBIA` | `admin@bancolombia.com` |
| NU | `NU` | `admin@nu.com` |
| Banco Horizonte | `BANCO_HORIZONTE` | `admin@horizonte.com` |

También existen `operador@horizonte.com`, `auditor@horizonte.com` y equivalentes en Nequi.

## Login por institución

```bash
curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "tenant_code": "BANCO_HORIZONTE",
    "email": "admin@horizonte.com",
    "password": "Admin123!"
  }'
```

La respuesta incluye `access_token`, `user` y `tenant` (con `primary_color`, `secondary_color` y `logo_url` para que el cliente pueda aplicar el tema de cada banco). El cliente debe guardar un token por `tenant_code` y enviar `Authorization: Bearer <token>` en las llamadas.

## Operaciones financieras

Depósitos, retiros y transferencias requieren el header `Idempotency-Key`:

```bash
curl -s http://localhost:8080/api/v1/transactions/deposits \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 7f4c3d2e-1111-2222-3333-444455556666' \
  -d '{
    "account_id": "30000000-0000-0000-0000-000000000001",
    "amount": "100.00",
    "currency": "USD",
    "description": "Depósito demo"
  }'
```

- La misma llave con el mismo cuerpo devuelve la respuesta original.
- La misma llave con otro cuerpo devuelve `409 IDEMPOTENCY_CONFLICT`.
- Los saldos usan `NUMERIC(19,4)` y `BigDecimal`; las tasas usan `NUMERIC(19,8)`.
- Las cuentas se bloquean con `PESSIMISTIC_WRITE` al operar y no se permite sobregiro en el MVP.

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Login por institución |
| `GET` | `/api/v1/auth/me` | Usuario y tenant del token |
| `GET/POST` | `/api/v1/tenants` | Instituciones (crear solo `PLATFORM_ADMIN`) |
| `GET/POST` | `/api/v1/users` | Usuarios del tenant |
| `GET/POST` | `/api/v1/customers` | Clientes del tenant |
| `GET/POST` | `/api/v1/accounts` | Cuentas del tenant |
| `GET/PUT` | `/api/v1/settings` | Configuración del tenant |
| `GET/POST` | `/api/v1/settings/exchange-rates` | Tasas de cambio |
| `POST` | `/api/v1/transactions/deposits` | Depósito idempotente |
| `POST` | `/api/v1/transactions/withdrawals` | Retiro idempotente |
| `POST` | `/api/v1/transactions/transfers` | Transferencia idempotente |
| `POST` | `/api/v1/transactions/transfers/preview` | Cálculo de tasa/comisión |
| `GET` | `/api/v1/transactions` | Historial con filtros |
| `GET` | `/api/v1/transactions/{id}` | Detalle de transacción |
| `GET` | `/api/v1/audit-logs` | Auditoría del tenant |
| `GET/POST` | `/api/v1/webhooks` | Configuración de webhooks |
| `GET` | `/api/v1/webhooks/events` | Outbox de eventos |

Todas las respuestas usan el sobre:

```json
{
  "success": true,
  "code": "DEPOSIT_COMPLETED",
  "message": "Depósito realizado correctamente",
  "data": {},
  "correlation_id": "..."
}
```

## Seguridad y aislamiento

- JWT HS256 con claims `sub`, `tenant_id`, `email`, `roles`, `iss`.
- El `tenant_id` se toma siempre del JWT; nunca del cuerpo o query params.
- `PLATFORM_ADMIN` pertenece al tenant reservado `PLATFORM`.
- Roles: `PLATFORM_ADMIN`, `TENANT_ADMIN`, `OPERATOR`, `AUDITOR`.
- El cliente puede almacenar varios tokens y elegir la sesión activa. El backend no mantiene sesiones de servidor: cada token es independiente y está atado a su institución.

## Build y pruebas

```bash
# Compilar sin tests
docker run --rm -v "$PWD":/workspace -v "$HOME/.m2":/root/.m2 \
  -w /workspace maven:3.9-eclipse-temurin-21 \
  mvn -B -DskipTests package

# Tests
docker run --rm -v "$PWD":/workspace -v "$HOME/.m2":/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w /workspace maven:3.9-eclipse-temurin-21 \
  mvn -B test
```

Los tests de integración usan Testcontainers + PostgreSQL. Requieren Docker accesible desde el contenedor de Maven.

## Producción

- Usar `SPRING_PROFILES_ACTIVE=prod`.
- Definir `JWT_SECRET` aleatorio de al menos 32 bytes.
- No usar el seed `V100` (solo está en `db/dev`).
- Configurar TLS en el proxy inverso.
- Revisar `docs/implementation-plan.md` para el backlog (refresh tokens, rate limiting, RLS, conciliación, etc.).
