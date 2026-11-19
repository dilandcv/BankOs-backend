# BankOS — Backend Service (`BankOs-backend`)

Backend multi-banco y multi-tenant de **BankOS**, desarrollado con **Java 21**, **Spring Boot 3**, **PostgreSQL** y **Redis**. Implementa el modelo de arquitectura bancaria robusta con **una sesión por institución**, aislamiento multi-tenant por token JWT, transacciones financieras atómicas e idempotentes, bloqueos pesimistas y auditoría completa.

**Repositorios del Ecosistema:**
- Backend (Este repositorio): [BankOs-backend](https://github.com/dilandcv/BankOs-backend)
- Mobile/Frontend: [BanckOs-mobile](https://github.com/dilandcv/BanckOs-mobile)
- Contratos Oficiales: [BankOs-contracts](https://github.com/dilandcv/BankOs-contracts)

---

## 1. ¿Cuál repositorio debo clonar si quiero probar el proyecto completo?

Para probar el proyecto completo funcionando de extremo a extremo (Frontend móvil/web interactuando con los servicios reales, base de datos y cache), necesitas clonar **dos repositorios**:

1. **`BankOs-backend` (este repositorio):** Contiene los microservicios, la lógica financiera, las migraciones de base de datos Flyway y la configuración de Docker Compose.
2. **[BanckOs-mobile](https://github.com/dilandcv/BanckOs-mobile):** Contiene la aplicación móvil y web desarrollada en Flutter con Riverpod y GoRouter.

### ¿Para qué es el tercer repositorio (`BankOs-contracts`)?
El repositorio `BankOs-contracts` almacena el contrato oficial **OpenAPI 3.1.0** (`openapi.yaml`). Actúa como la **única fuente de verdad** compartida entre ambos equipos. Ningún cambio de endpoints o estructuras JSON se realiza sin pasar por la gobernanza de contratos. Tanto este backend como la aplicación móvil cuentan con pruebas automáticas de paridad que aseguran que el contrato local sea idéntico bit a bit (SHA-256) al del repositorio de contratos.

---

## 2. Guía de Ejecución Rápida del Sistema Completo

Para poner en marcha todo el sistema BankOS:

### Paso 1: Iniciar el Backend (Servicios, BD y Cache)

Desde este repositorio (`BankOs-backend`):

```bash
# 1. Copiar variables de entorno
cp .env.example .env

# 2. Levantar los contenedores en segundo plano
docker compose up --build -d
```

> **Nota sobre puertos:**
> - **API Spring Boot:** `http://localhost:8080`
> - **PostgreSQL:** Expuesto en el puerto host `55432` (para evitar conflictos si ya tienes un PostgreSQL local en el puerto `5432`). Dentro de la red Docker, se comunica en el puerto interno `5432`.
> - **Redis Cache:** Puerto `6379`.

#### Verificación de Salud:
```bash
# Healthcheck de Actuator
curl -s http://localhost:8080/actuator/health
# Respuesta esperada: {"status":"UP"}

# Documentación interactiva Swagger UI (perfil dev):
# Abrir en el navegador: http://localhost:8080/swagger-ui.html
```

### Paso 2: Iniciar el Cliente Flutter

En una nueva terminal, clona y ejecuta la aplicación móvil/web:

```bash
git clone https://github.com/dilandcv/BanckOs-mobile.git
cd BanckOs-mobile

flutter pub get

# Opción A: Ejecutar en navegador Web (Google Chrome)
flutter run -d chrome

# Opción B: Ejecutar en emulador Android conectado
flutter run

# Opción C: Ejecutar en dispositivo físico en la misma red Wi-Fi
flutter run --dart-define=API_BASE_URL=http://<IP_LOCAL_DE_TU_PC>:8080
```

---

## 3. Credenciales Demo para Pruebas

El perfil `dev` ejecuta automáticamente la migración `V100__dev_seed.sql`, precargando instituciones bancarias, usuarios con diferentes roles, cuentas y saldos iniciales.

**Contraseña universal para todas las cuentas demo:** `Admin123!`

| Institución | Código (`tenant_code`) | Email | Rol |
|---|---|---|---|
| **Banco Horizonte** | `BANCO_HORIZONTE` | `admin@horizonte.com` | `TENANT_ADMIN` |
| Banco Horizonte | `BANCO_HORIZONTE` | `operador@horizonte.com` | `OPERATOR` |
| Banco Horizonte | `BANCO_HORIZONTE` | `auditor@horizonte.com` | `AUDITOR` |
| **Nequi** | `NEQUI` | `admin@nequi.com` | `TENANT_ADMIN` |
| **Daviplata** | `DAVIPLATA` | `admin@daviplata.com` | `TENANT_ADMIN` |
| **PayPal** | `PAYPAL` | `admin@paypal.com` | `TENANT_ADMIN` |
| **Bancolombia** | `BANCOLOMBIA` | `admin@bancolombia.com` | `TENANT_ADMIN` |
| **NU** | `NU` | `admin@nu.com` | `TENANT_ADMIN` |
| **BankOS Platform** | `PLATFORM` | `platform@bankos.com` | `PLATFORM_ADMIN` |

---

## 4. Arquitectura y Reglas Financieras

### Autenticación por Institución
El backend no mantiene sesiones en servidor. Cada solicitud se autentica mediante un token JWT HS256 emitido tras validar `tenant_code + email + password`:
```bash
curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "tenant_code": "BANCO_HORIZONTE",
    "email": "admin@horizonte.com",
    "password": "Admin123!"
  }'
```
El token incluye los claims `sub`, `tenant_id`, `email`, `roles` e `iss`. El `tenant_id` se extrae exclusivamente del JWT verificado, impidiendo cualquier escalamiento horizontal entre bancos.

### Operaciones Financieras e Idempotencia
Depósitos, retiros y transferencias requieren obligatoriamente la cabecera `Idempotency-Key` (UUIDv4):
```bash
curl -s http://localhost:8080/api/v1/transactions/deposits \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 7f4c3d2e-1111-2222-3333-444455556666' \
  -d '{
    "account_id": "30000000-0000-0000-0000-000000000001",
    "amount": "100.00",
    "currency": "USD",
    "description": "Depósito seguro"
  }'
```
- **Idempotencia estricta:** Reintentos con la misma clave y el mismo cuerpo devuelven la respuesta original sin re-ejecutar el débito/crédito.
- **Detección de conflictos:** La misma clave con un payload distinto retorna `409 IDEMPOTENCY_CONFLICT`.
- **Integridad monetaria:** Todos los saldos se manejan con `NUMERIC(19,4)` y `BigDecimal` en Java. Prohibido el uso de flotantes binarios (`double`/`float`).
- **Concurrencia:** Bloqueos pesimistas de escritura (`PESSIMISTIC_WRITE`) sobre las cuentas durante las transacciones para evitar condiciones de carrera o saldos negativos.

---

## 5. Endpoints Principales (API v1)

Todas las respuestas cumplen con el sobre oficial definido en el contrato OpenAPI:
```json
{
  "success": true,
  "code": "OPERATION_SUCCESS",
  "message": "Operación completada exitosamente",
  "data": {},
  "correlation_id": "c9bf9e57-1685-4c89-bafb-ff5af830be8a"
}
```

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Login por institución (retorna JWT y branding) |
| `GET` | `/api/v1/auth/me` | Información del usuario autenticado y su tenant |
| `GET/POST` | `/api/v1/tenants` | Listado y creación de bancos (solo `PLATFORM_ADMIN`) |
| `GET/POST` | `/api/v1/users` | Gestión de usuarios del tenant |
| `GET/POST` | `/api/v1/customers` | Gestión de clientes bancarios |
| `GET/POST` | `/api/v1/accounts` | Cuentas asociadas al banco |
| `GET/PUT` | `/api/v1/settings` | Configuración y parámetros de la entidad |
| `GET/POST` | `/api/v1/settings/exchange-rates` | Tasas de cambio entre divisas |
| `POST` | `/api/v1/transactions/deposits` | Depósito idempotente |
| `POST` | `/api/v1/transactions/withdrawals` | Retiro idempotente |
| `POST` | `/api/v1/transactions/transfers` | Transferencia entre cuentas con idempotencia |
| `POST` | `/api/v1/transactions/transfers/preview` | Cotización de comisiones y conversión de divisas |
| `GET` | `/api/v1/transactions` | Historial paginado con filtros de fecha y tipo |
| `GET` | `/api/v1/transactions/{id}` | Detalle con asientos contables de partida doble |
| `GET` | `/api/v1/audit-logs` | Trazabilidad y auditoría de eventos |
| `GET/POST` | `/api/v1/webhooks` | Suscripciones de webhooks |
| `GET` | `/api/v1/webhooks/events` | Outbox pattern para entrega de eventos |

---

## 6. Pruebas Automatizadas y Paridad de Contrato

El backend incluye pruebas de integración y validación estricta de paridad del contrato OpenAPI (`openapi.yaml`):

```bash
# Ejecutar suite de pruebas con Maven en contenedor Docker
docker run --rm -v "$PWD":/workspace -v "$HOME/.m2":/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w /workspace maven:3.9-eclipse-temurin-21 \
  mvn -B test
```

La suite incluye:
- `ContractParityTest.java`: Verifica que `openapi.yaml` local coincida exactamente con la suma criptográfica SHA-256 oficial (`f454d326...`).
- `OpenApiExportIntegrationTest.java`: Valida la compatibilidad del motor Springdoc con la especificación.
- `TransactionApiIntegrationTest.java`: Pruebas de integración con Testcontainers para depósitos, transferencias, idempotencia y control de concurrencia.
- `MoneyUtilsTest.java`: Pruebas unitarias de redondeo y aritmética financiera en `BigDecimal`.
