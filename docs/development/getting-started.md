# Guía de inicio

## Estado

- **Implementado:** backend Spring Boot 3.5, Java 21, Maven, PostgreSQL 16, Redis 7, Flyway, JWT, Docker Compose y Swagger.
- **Ejecutable de forma independiente:** sí. No requiere `mobile/`.
- **Perfil de demostración:** `dev`, con migraciones y seed de instituciones demo.

## Requisitos

| Herramienta | Versión | Uso |
|---|---|---|
| Docker / Docker Compose | Reciente | Ejecutar API + PostgreSQL + Redis |
| Java | 21 (opcional) | Ejecución local sin Docker |
| Maven | 3.9+ (opcional) | Build local sin Docker |
| curl / cliente HTTP | Cualquiera | Pruebas manuales de la API |

No se requiere Node, Flutter ni el frontend.

## Estructura

```text
backend/
├── src/main/java/com/bankos/
│   ├── common/          # seguridad, tenancy, errores, respuestas
│   └── modules/         # auth, tenants, users, customers, accounts,
│                        # settings, transactions, audit, webhooks
├── src/main/resources/
│   ├── application*.yml
│   └── db/
│       ├── migration/   # V1–V6 (todos los perfiles)
│       └── dev/         # V100 seed demo (solo perfil dev)
├── src/test/java/       # unitarios + integración Testcontainers
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── docs/
```

## Ejecución con Docker Compose

```bash
cd backend
cp .env.example .env
docker compose up --build
```

> Si el puerto `5432` está ocupado por otro PostgreSQL, ejecuta `DB_PORT=55432 docker compose up --build`. El backend sigue conectándose al PostgreSQL interno en `5432`.

Servicios:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

El perfil `dev` carga el seed `V100__dev_seed.sql`. Para detener:

```bash
docker compose down
# Eliminar también la base de datos:
docker compose down -v
```

## Credenciales demo

Contraseña para todos los usuarios demo: `Admin123!`

| Institución | `tenant_code` | Email |
|---|---|---|
| BankOS Platform | `PLATFORM` | `platform@bankos.com` |
| Nequi | `NEQUI` | `admin@nequi.com` |
| Daviplata | `DAVIPLATA` | `admin@daviplata.com` |
| PayPal | `PAYPAL` | `admin@paypal.com` |
| Bancolombia | `BANCOLOMBIA` | `admin@bancolombia.com` |
| NU | `NU` | `admin@nu.com` |
| Banco Horizonte | `BANCO_HORIZONTE` | `admin@horizonte.com` |

También existen `operador@horizonte.com`, `auditor@horizonte.com`, `operador@nequi.com` y `auditor@nequi.com`.

## Smoke test

### 1. Login

```bash
TOKEN=$(curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenant_code":"BANCO_HORIZONTE","email":"admin@horizonte.com","password":"Admin123!"}' \
  | jq -r '.data.access_token')

echo "$TOKEN"
```

### 2. Usuario y tenant actuales

```bash
curl -s http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 3. Depósito idempotente

```bash
curl -s http://localhost:8080/api/v1/transactions/deposits \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-deposit-001' \
  -d '{
    "account_id": "30000000-0000-0000-0000-000000000001",
    "amount": "100.00",
    "currency": "USD",
    "description": "Depósito demo"
  }' | jq
```

Repetir la misma petición con la misma llave devuelve la respuesta original. Cambiar el monto con la misma llave devuelve `409 IDEMPOTENCY_CONFLICT`.

### 4. Transferencia entre monedas

La cuenta `30000000-0000-0000-0000-000000000001` es USD y la `30000000-0000-0000-0000-000000000003` es EUR, con tasa demo configurada:

```bash
curl -s http://localhost:8080/api/v1/transactions/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-transfer-001' \
  -d '{
    "source_account_id": "30000000-0000-0000-0000-000000000001",
    "target_account_id": "30000000-0000-0000-0000-000000000003",
    "amount": "100.00",
    "currency": "USD",
    "description": "Transferencia demo"
  }' | jq
```

### 5. Aislamiento entre instituciones

Un token de `NEQUI` no puede leer una cuenta de `BANCO_HORIZONTE`: responde `404 ACCOUNT_NOT_FOUND` sin revelar que existe.

## Build y pruebas sin Compose

Los comandos usan Docker para no depender de Java/Maven locales.

```bash
# Compilar y empaquetar sin tests
docker run --rm \
  -v "$PWD":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B -DskipTests package

# Pruebas (Testcontainers requiere el socket de Docker)
docker run --rm \
  -v "$PWD":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B test
```

Artefacto: `target/bankos-backend-0.1.0-SNAPSHOT.jar`.

Pruebas incluidas:

- `MoneyUtilsTest`: redondeo y escalas monetarias.
- `TransactionApiIntegrationTest`: login, depósito, replay idempotente, conflicto de idempotencia, historial con filtros opcionales y aislamiento entre tenants.

## Perfiles

| Perfil | Uso | Seed | Scheduling | DDL |
|---|---|---|---|---|
| `dev` | Desarrollo local | Sí (`db/dev`) | Sí | `validate` |
| `test` | Tests | No | Deshabilitado | `validate` |
| `prod` | Producción | No | Sí | `validate` |

## Troubleshooting

| Problema | Causa probable | Solución |
|---|---|---|
| `401 INVALID_CREDENTIALS` | `tenant_code`, email o contraseña incorrectos | Verificar que el email pertenezca a ese tenant. |
| `401 UNAUTHORIZED` | Token ausente, expirado o `JWT_SECRET` distinto | Volver a hacer login con el mismo secreto. |
| `400 IDEMPOTENCY_KEY_REQUIRED` | Falta `Idempotency-Key` | Enviar una llave única por operación. |
| `409 IDEMPOTENCY_CONFLICT` | Misma llave con otro cuerpo | Usar una llave nueva para una operación distinta. |
| `422 EXCHANGE_RATE_NOT_FOUND` | No hay tasa para el par de monedas | Crear la tasa con `POST /api/v1/settings/exchange-rates`. |
| Backend no arranca en Compose | PostgreSQL/Redis aún no están sanos | Revisar `docker compose ps` y logs. |
| `Bind for 0.0.0.0:5432 failed` | Otro PostgreSQL ocupa el puerto | Ejecutar con `DB_PORT=55432`. |
| Tests fallan al iniciar Testcontainers | Docker no accesible desde el contenedor Maven | Montar `/var/run/docker.sock`; se usa Testcontainers `1.21.4` por compatibilidad con Docker Engine 29+. |

## Documentación relacionada

- [README del backend](../../README.md)
- [Visión general de la API](../api/api-overview.md)
- [Contratos de API](../api/api-contracts.md)
- [Autenticación](../security/authentication.md)
- [Multitenancy](../security/multitenancy.md)
- [Docker](../infrastructure/docker-setup.md)
- [Plan de implementación](../implementation-plan.md)
- [Modelo multi-banco](../../../docs/architecture/multi-bank-model.md)
