# Plan de implementación del backend — BankOS

> **Estado:** backend MVP implementado. Este documento conserva el plan por fases, las decisiones y el backlog pendiente; el código vive en `backend/src/`.
>
> **Fecha:** 2026-11-16
>
> **Base:** auditoría del repositorio y documentación existente en `docs/` y `backend/docs/`.
>
> **Estado de ejecución (2026-11-17):** proyecto Maven, seguridad JWT, Flyway, módulos de dominio, idempotencia, auditoría, webhooks/outbox y Docker ya implementados. `mvn -B test` pasa con 7 pruebas y el backend responde `UP` con PostgreSQL y Redis. `mobile/` sigue sin modificarse. Ver [README del backend](README.md) y [modelo multi-banco](../../docs/architecture/multi-bank-model.md).

## 0. Estado actual de la implementación

| Área | Estado | Evidencia |
|---|---|---|
| Build Maven / Java 21 / Spring Boot 3.5 | Implementado | `mvn -B -DskipTests package` genera el JAR. |
| Seguridad JWT, BCrypt 12, RBAC | Implementado | Login, `/auth/me`, roles y filtro de tenant. |
| Multitenancy por `tenant_id` del JWT | Implementado | Repositorios y servicios filtran por tenant; prueba de aislamiento. |
| Dinero `NUMERIC(19,4)` / `BigDecimal` | Implementado | `MoneyUtils` y entidades monetarias. |
| Depósitos, retiros, transferencias y preview | Implementado | Controladores, servicios y asientos contables. |
| Idempotencia con advisory lock | Implementado | Replay y conflicto cubiertos por prueba de integración. |
| Auditoría y outbox de webhooks | Implementado | Tablas, servicios y dispatcher programado. |
| Flyway + seed multi-banco | Implementado | `V1`–`V6` y `V100` con 7 instituciones demo. |
| Docker Compose | Implementado | Servicios backend, PostgreSQL y Redis con healthchecks. |
| Swagger / OpenAPI | Implementado | `/swagger-ui.html` y `/v3/api-docs`. |
| Pruebas | Implementado (base) | 3 unitarias + 4 de integración; faltan concurrencia, contrato y carga. |
| CI/CD | Pendiente | No hay workflow de GitHub Actions. |
| Maven Wrapper | Pendiente | Se usa Maven del sistema o imagen Docker. |
| Refresh tokens, revocación, rate limiting | Pendiente | Documentado en el backlog. |
| RLS de PostgreSQL y cifrado por tenant | Pendiente | Aislamiento lógico explícito en el MVP. |
| Reconciliación contable y cierre diario | Pendiente | Fuera del alcance MVP. |
| Integración con `mobile/` | Fuera de alcance | El frontend no fue modificado. |

## 1. Diagnóstico inicial del repositorio (histórico, antes de implementar)

| Área | Estado real observado | Consecuencia para el plan |
|---|---|---|
| Backend Java/Spring Boot | **No implementado**. `backend/` solo contiene documentación. | Se parte de cero: build, código, configuración, migraciones y pruebas. |
| Maven / Java | No hay `pom.xml`, `mvnw` ni código fuente. En el entorno local no están instalados `java` ni `mvn`. | El primer paso técnico es instalar JDK 21 o construir mediante Docker. |
| PostgreSQL / Flyway | No configurados. No hay migraciones ni esquema. | El esquema debe definirse con Flyway antes de implementar módulos. |
| Redis | No configurado. | Se prepara la conexión y el uso auxiliar; PostgreSQL sigue siendo fuente de verdad. |
| Docker | Hay Docker y Docker Compose disponibles; no hay `Dockerfile` ni compose en el repo. | La infraestructura local puede implementarse temprano y validar el backend de forma independiente. |
| API REST | Solo contratos propuestos en documentación. | Se deben congelar contratos y códigos de error antes de codificar controladores. |
| Seguridad / multitenancy | Solo diseño propuesto. | Debe implementarse desde el inicio; no es un añadido posterior. |
| Frontend Flutter | Esqueleto con datos mock. No consume backend. | **Fuera de alcance**. No se modifica ni se conecta en esta fase. |
| CI/CD | No existe. | Se propone GitHub Actions para build, pruebas y build de imagen. |
| Git | Rama `develop` y commit inicial. | Trabajar en ramas `feature/*` y PRs pequeños por fase. |

**Conclusión:** el backend no está hecho. El plan siguiente asume implementación desde cero, por fases, con entregables verificables y sin integración con el frontend.

### 1.1 Modelo multi-banco adoptado (Opción A)

La app es un cliente único con varias instituciones. Cada usuario pertenece a un tenant y hace login por institución con `tenant_code + email + password`. La app guarda un token por banco y cambia la sesión activa sin volver a autenticarse. El JWT contiene `tenant_id`, `email` y `roles`; el backend nunca acepta el tenant desde body/path/query.

Consecuencias para este plan:

- `users` mantiene `tenant_id` obligatorio y email único por tenant.
- `tenants` incluye branding (`primary_color`, `secondary_color`, `logo_url`) para que el cliente aplique el tema por banco.
- No se implementa una “sesión global” ni una tabla de sesiones en el MVP; el cliente almacena los tokens.
- El seed de desarrollo incluye `PLATFORM`, `NEQUI`, `DAVIPLATA`, `PAYPAL`, `BANCOLOMBIA`, `NU` y `BANCO_HORIZONTE`.
- El frontend multi-banco (selector, tema dinámico, almacenamiento multi-token, Dio) queda documentado como pendiente y **fuera de este backend**.

---

## 2. Alcance y límites

### Incluido

- Backend monolito modular en Java 21 / Spring Boot 3.5.x / Maven.
- API REST JSON versionada en `/api/v1`.
- PostgreSQL como fuente de verdad, con migraciones Flyway.
- Redis como apoyo operativo (cache/idempotencia auxiliar/rate limiting futuro), sin sustituir PostgreSQL.
- Seguridad con Spring Security + JWT y aislamiento multitenant.
- Depósitos, retiros, transferencias, preview, historial, idempotencia, auditoría y webhooks/outbox.
- Dockerfile y Docker Compose para ejecutar backend + PostgreSQL + Redis de forma independiente.
- Pruebas unitarias, de integración, de seguridad y de consistencia financiera.
- CI para compilar, probar y construir la imagen.

### Excluido de esta fase

- Cambios en `mobile/`, cliente Dio, pantallas o navegación Flutter.
- Integración real frontend-backend.
- Despliegue a producción, TLS, dominio, orquestador y gestión de secretos productiva.
- Decisiones de negocio pendientes que no bloqueen el MVP (ver sección 4).
- Facturación, tarjetas, préstamos, KYC, cumplimiento regulatorio avanzado.

---

## 3. Principios rectores

1. **Backend independiente:** debe compilar, arrancar y probarse sin Flutter ni servicios externos del frontend.
2. **Contrato primero:** DTOs, códigos de error y OpenAPI se definen antes o junto con el controlador.
3. **Monolito modular:** paquetes por dominio, dependencias unidireccionales, sin ciclos.
4. **Capas estrictas:** controller → service → repository; entidades JPA nunca se exponen.
5. **Tenant desde el contexto autenticado:** nunca desde body, path o query del cliente.
6. **Dinero con `BigDecimal` y `NUMERIC`:** prohibido `float`/`double`.
7. **Operaciones financieras atómicas e idempotentes:** transacción SQL, bloqueos y `Idempotency-Key`.
8. **Trazabilidad:** `X-Correlation-ID` en request/response/logs y auditoría persistida.
9. **Pruebas como criterio de Done:** cada fase incluye pruebas y evidencia de ejecución.
10. **Sin secretos en git:** `.env.example` con placeholders; `.env` ignorado.

---

## 4. Decisiones a congelar antes de implementar (Fase F0)

Estas decisiones se documentan como ADR cortos o en este mismo plan. Se propone un valor por defecto para no bloquear el arranque; el equipo puede ajustarlo.

| ID | Decisión | Recomendación para el MVP | Impacto |
|---|---|---|---|
| D1 | Paquete base y artefacto | `com.bankos`, artefacto `bankos-backend`, estructura `common/*` + `modules/*`. | Estructura de todo el código. |
| D2 | Precisión monetaria | `NUMERIC(19,4)` para montos/saldos/comisiones; `NUMERIC(19,8)` para tasas; `BigDecimal` con escala 4 y `RoundingMode.HALF_UP`. | Dinero, conversiones y reportes. |
| D3 | Descubierto | No permitido en MVP: `CHECK (balance >= 0)`. | Retiros y transferencias. |
| D4 | Login y tenant | **Opción A confirmada:** login con `tenant_code` + `email` + `password`; un usuario pertenece a un tenant; la app guarda un token por banco. El JWT incluye `tenant_id`, `email` y `roles`. | Contrato de autenticación y experiencia multi-banco. |
| D5 | Roles | Enum simple: `PLATFORM_ADMIN`, `TENANT_ADMIN`, `OPERATOR`, `AUDITOR`. Los `PLATFORM_ADMIN` pertenecen a un tenant reservado `PLATFORM` para mantener `tenant_id` obligatorio. RBAC completo con tablas queda para después. | Autorización y endpoints. |
| D6 | Multitenancy | Filtrado explícito por `tenant_id` en repositorios + `TenantContext`; pruebas de aislamiento obligatorias. Sin RLS en MVP. | Seguridad y consultas. |
| D7 | Idempotencia | Única por `(tenant_id, operation, idempotency_key)`; guardar `request_hash` y snapshot de respuesta. Misma clave + mismo body → replay; misma clave + body distinto → `409 IDEMPOTENCY_CONFLICT`. | Depósitos, retiros y transferencias. |
| D8 | Webhooks | Patrón Outbox: evento en la misma transacción; dispatcher programado en la misma app para el MVP; worker separado después. | Integraciones externas. |
| D9 | Formato API | Envelope `success/code/message/data/correlation_id`; JSON `snake_case`; `BigDecimal` serializado como string; fechas ISO-8601. | Contratos y cliente futuro. |
| D10 | Códigos de error | Catálogo central `ErrorCode`; validación 400, auth 401, permisos 403, no encontrado 404, conflicto/idempotencia 409, reglas financieras 422, interno 500. | Consistencia frontend/backend. |
| D11 | Paginación | `page`, `size`, `sort`; respuesta con `items`, `page`, `size`, `total_elements`, `total_pages`. | Listados. |
| D12 | Migraciones | Flyway es dueño del esquema; `ddl-auto=validate`; migraciones inmutables. | Base de datos. |
| D13 | Asientos contables | MVP: depósito/retiro generan un asiento sobre la cuenta; transferencia genera débito y crédito. Post-MVP: evaluar cuentas de sistema para doble partida estricta. | `transaction_entries` y reconstrucción de saldo. |
| D14 | Seed | Datos demo solo en perfil `dev`, nunca en `prod`; credenciales ficticias documentadas. Incluye tenants `PLATFORM`, `NEQUI`, `DAVIPLATA`, `PAYPAL`, `BANCOLOMBIA`, `NU` y `BANCO_HORIZONTE`. | Pruebas manuales e integración futura. |

**Criterio de salida F0:** decisiones D1-D14 registradas y contratos MVP congelados. Sin esto no se empieza F1.

---

## 5. Mapa de fases

| Fase | Área / responsabilidad | Objetivo | Depende de | Prioridad | ¿MVP? |
|---|---|---|---|---|---|
| **F0** | Decisiones y contratos | Congelar decisiones, endpoints, errores y esquema lógico. | — | P0 | Sí |
| **F1** | Build / tooling | Proyecto Maven, Java 21, estructura base, perfiles. | F0 | P0 | Sí |
| **F2** | Docker / infraestructura | Dockerfile, Compose, PostgreSQL, Redis, healthchecks. | F1 | P0 | Sí |
| **F3** | Base de datos / Flyway | Esquema inicial, constraints, índices, seed dev. | F2 | P0 | Sí |
| **F4** | Núcleo API / transversales | Envelope, errores, correlation ID, tenant context, OpenAPI. | F3 | P0 | Sí |
| **F5** | Seguridad / multitenancy | Login, JWT, filtros, roles, `/auth/me`, aislamiento. | F4 | P0 | Sí |
| **F6** | Módulos de dominio | Tenants, users, customers, accounts, settings, idempotency, transactions, audit, webhooks. | F5 | P0/P1 | Parcial |
| **F7** | Consistencia financiera | Bloqueos, concurrencia, redondeo, reconciliación. | F6 | P0 | Sí |
| **F8** | Pruebas / calidad | Unitarias, integración, seguridad, concurrencia, contrato. | F6/F7 | P0 | Sí |
| **F9** | Observabilidad / operación | Actuator, logs, perfiles, validación de configuración. | F4 | P1 | Sí |
| **F10** | CI/CD | GitHub Actions: build, tests, imagen. | F8 | P1 | Sí |
| **F11** | Documentación / preparación de integración | OpenAPI final, README, colección HTTP, checklist de integración futura. | Todas | P0 | Sí |

**Esfuerzo orientativo (no compromiso):** el backend completo ronda 70–110 horas ideales. Para una hackatón de 10 horas se debe ejecutar la ruta MVP definida en la sección 8, no el plan completo.

---

## 6. Fases detalladas

### F0 — Decisiones, contratos y esquema lógico

**Tecnología/responsabilidad:** arquitectura, API, producto técnico.

**Tareas**

- [ ] Crear `backend/docs/decisions/` con ADRs cortos para D1-D14.
- [ ] Congelar el catálogo inicial de `ErrorCode` y su mapeo HTTP.
- [ ] Congelar los endpoints MVP y sus DTOs request/response.
- [ ] Definir el esquema lógico definitivo (tablas, columnas, tipos, constraints, índices).
- [ ] Definir reglas de comisión y conversión del MVP.
- [ ] Definir semántica exacta de idempotencia (replay, conflicto, en progreso).
- [ ] Definir datos seed de desarrollo (tenant, usuarios, cliente, cuentas).
- [ ] Actualizar `backend/docs/api/api-contracts.md` marcando lo que se implementará.

**Entregables**

- ADRs D1-D14.
- Contrato MVP con endpoints, headers, ejemplos y errores.
- Esquema lógico validado.
- Lista de datos seed.

**Criterio de salida**

- No quedan decisiones P0 abiertas que impidan crear migraciones o controladores.
- Los contratos MVP son revisados por al menos otra persona.

---

### F1 — Build, tooling y esqueleto Spring Boot

**Tecnología/responsabilidad:** Java 21, Maven, Spring Boot, estructura del proyecto.

**Tareas**

- [ ] Instalar JDK 21 y Maven 3.9+ en el entorno, o usar la imagen Maven de Docker para generar el wrapper.
- [ ] Crear `backend/pom.xml` con Spring Boot 3.5.x, Java 21 y dependencias:
  - [ ] `spring-boot-starter-web`
  - [ ] `spring-boot-starter-security`
  - [ ] `spring-boot-starter-data-jpa`
  - [ ] `spring-boot-starter-validation`
  - [ ] `spring-boot-starter-data-redis`
  - [ ] `spring-boot-starter-actuator`
  - [ ] `flyway-core` + `flyway-database-postgresql`
  - [ ] `postgresql` (runtime)
  - [ ] `spring-boot-starter-oauth2-resource-server` (JWT/Jose)
  - [ ] `springdoc-openapi-starter-webmvc-ui`
  - [ ] Testing: `spring-boot-starter-test`, `spring-security-test`, `testcontainers` PostgreSQL, JUnit 5, AssertJ, Mockito.
- [ ] Generar y commitear Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`).
- [ ] Crear `BankOsApplication.java` en `com.bankos`.
- [ ] Crear estructura de paquetes:
  ```text
  com.bankos
  ├── common
  │   ├── config
  │   ├── security
  │   ├── tenancy
  │   ├── exception
  │   ├── response
  │   └── web
  └── modules
      ├── tenants
      ├── authentication
      ├── users
      ├── customers
      ├── accounts
      ├── transactions
      ├── idempotency
      ├── settings
      ├── audit
      └── webhooks
  ```
- [ ] Configurar perfiles `dev`, `test`, `prod` con `application.yml` y overrides.
- [ ] Configurar `spring.jpa.hibernate.ddl-auto=validate`, Flyway enabled y Jackson.
- [ ] Crear `backend/.env.example` con placeholders.
- [ ] Actualizar `.gitignore` si falta algo (target, `.env`, logs).
- [ ] Añadir `backend/README.md` con comandos de build/run/test.

**Entregables**

- Proyecto compilable y ejecutable con perfil `dev` (aunque falle la conexión a BD si aún no está F3).
- `./mvnw -q test` ejecutable.
- Estructura modular vacía pero estable.

**Criterio de salida**

- `./mvnw -q -DskipTests package` genera el JAR.
- La aplicación arranca con PostgreSQL/Redis disponibles y `ddl-auto=validate` sin entidades aún.

---

### F2 — Docker e infraestructura local

**Tecnología/responsabilidad:** Docker, Docker Compose, PostgreSQL, Redis.

**Tareas**

- [ ] Crear `backend/Dockerfile` multi-stage:
  - [ ] Etapa build con `maven:3.9-eclipse-temurin-21`.
  - [ ] Etapa runtime con `eclipse-temurin:21-jre-alpine`.
  - [ ] Usuario no root.
  - [ ] `HEALTHCHECK` contra `/actuator/health`.
  - [ ] `ENTRYPOINT` configurable por variables de entorno.
- [ ] Crear `backend/.dockerignore`.
- [ ] Crear `backend/docker-compose.yml` con:
  - [ ] `postgres:16` con volumen persistente y healthcheck `pg_isready`.
  - [ ] `redis:7` con healthcheck `redis-cli ping`.
  - [ ] `app` con build del backend, `depends_on` con condición `service_healthy`, puerto configurable.
  - [ ] Red interna y variables desde `.env`.
  - [ ] Perfil `infra` para levantar solo PostgreSQL/Redis durante desarrollo Maven.
- [ ] Crear `backend/.env.example` con:
  - [ ] `SPRING_PROFILES_ACTIVE`, `APP_PORT`.
  - [ ] `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`.
  - [ ] `REDIS_HOST`, `REDIS_PORT`.
  - [ ] `JWT_SECRET`, `JWT_EXPIRATION_MS`.
- [ ] Documentar comandos:
  - [ ] `docker compose --profile infra up -d`.
  - [ ] `docker compose up --build`.
  - [ ] `docker compose down` y limpieza de volumen.
- [ ] Verificar que no se incluye ningún servicio ni archivo de `mobile/`.

**Entregables**

- Backend ejecutable con `docker compose up --build`.
- PostgreSQL y Redis con healthchecks funcionales.
- `.env.example` versionado y `.env` ignorado.

**Criterio de salida**

- `curl http://localhost:${APP_PORT}/actuator/health` responde `UP`.
- `docker compose ps` muestra servicios saludables.
- Un desarrollador puede levantar el backend sin instalar PostgreSQL/Redis localmente.

---

### F3 — Base de datos, Flyway y modelo físico

**Tecnología/responsabilidad:** PostgreSQL, Flyway, modelado de datos.

**Tareas**

- [ ] Definir convenciones de esquema:
  - [ ] UUID como PK.
  - [ ] `TIMESTAMPTZ` para fechas.
  - [ ] `NUMERIC(19,4)` para dinero y `NUMERIC(19,8)` para tasas.
  - [ ] `VARCHAR` + `CHECK` para estados, con `@Enumerated(EnumType.STRING)` en JPA.
  - [ ] Nombres en `snake_case`.
- [ ] Crear migraciones incrementales en `backend/src/main/resources/db/migration/`:
  - [ ] `V1__tenants_users.sql`: `tenants`, `users`.
  - [ ] `V2__customers_accounts.sql`: `customers`, `accounts`.
  - [ ] `V3__transactions_entries.sql`: `transactions`, `transaction_entries`.
  - [ ] `V4__settings_exchange_rates.sql`: `tenant_settings`, `exchange_rates`.
  - [ ] `V5__idempotency_audit.sql`: `idempotency_records`, `audit_logs`.
  - [ ] `V6__webhooks_outbox.sql`: `webhook_configs`, `outbox_events`.
- [ ] Definir constraints e índices:
  - [ ] `tenants.code` único.
  - [ ] `users(tenant_id, email)` único.
  - [ ] `customers(tenant_id, document_number)` único.
  - [ ] `accounts(tenant_id, account_number)` único.
  - [ ] `idempotency_records(tenant_id, operation, idempotency_key)` único.
  - [ ] `CHECK (amount > 0)`, `CHECK (balance >= 0)`, moneda ISO de 3 letras.
  - [ ] FK con `ON DELETE RESTRICT` para datos financieros.
  - [ ] Índices por `tenant_id`, `created_at`, `correlation_id`, `account_id`, `transaction_id`.
- [ ] Crear datos seed solo para `dev`:
  - [ ] Tenant reservado `PLATFORM` (`ACTIVE`) para administración de la plataforma.
  - [ ] Usuario `platform@bankos` (`PLATFORM_ADMIN`) en el tenant `PLATFORM`.
  - [ ] Tenant `BANCO_HORIZONTE` (`ACTIVE`).
  - [ ] Usuarios `admin@horizonte` (`TENANT_ADMIN`), `operador@horizonte` (`OPERATOR`), `auditor@horizonte` (`AUDITOR`).
  - [ ] Cliente María González.
  - [ ] Cuentas USD y EUR alineadas con `mobile/lib/shared/constants/mock_data.dart` (solo como referencia de datos, sin conectar mobile).
  - [ ] Tasas de cambio y settings iniciales.
- [ ] Configurar Flyway para no ejecutar seeds en `test`/`prod`.
- [ ] Añadir prueba de integración que arranque PostgreSQL con Testcontainers y aplique todas las migraciones.
- [ ] Actualizar `backend/docs/architecture/database-design.md` con el esquema implementado.

**Entregables**

- Esquema reproducible desde cero.
- Seed de desarrollo documentado.
- Prueba de migraciones en verde.

**Criterio de salida**

- Flyway aplica todas las migraciones sobre una BD vacía.
- `ddl-auto=validate` pasa con las entidades JPA del MVP.
- No hay tipos flotantes para dinero.

---

### F4 — Núcleo de API y componentes transversales

**Tecnología/responsabilidad:** Spring Web, Jackson, manejo de errores, OpenAPI, filtros.

**Tareas**

- [ ] Implementar `ApiResponse<T>` con `success`, `code`, `message`, `data`, `correlation_id`.
- [ ] Implementar `PageResponse<T>` con `items`, `page`, `size`, `total_elements`, `total_pages`.
- [ ] Crear `ErrorCode` como enum central y catálogo documentado.
- [ ] Crear excepciones de dominio:
  - [ ] `NotFoundException`, `ConflictException`, `BusinessRuleException`, `UnauthorizedException`, `ForbiddenException`.
- [ ] Implementar `GlobalExceptionHandler` con `@RestControllerAdvice`:
  - [ ] Validación Jakarta → 400 `VALIDATION_ERROR` con detalle por campo en `data`.
  - [ ] Errores de autenticación → 401.
  - [ ] Errores de autorización/tenant → 403.
  - [ ] No encontrado → 404.
  - [ ] Conflicto/idempotencia → 409.
  - [ ] Regla financiera → 422.
  - [ ] Error interno → 500 sin stack trace.
- [ ] Implementar `CorrelationIdFilter`:
  - [ ] Leer `X-Correlation-ID`; aceptar UUID válido; generar si falta; rechazar malformado con 400.
  - [ ] Propagar a MDC y a la respuesta.
- [ ] Implementar `TenantContext` y filtro de limpieza por request.
- [ ] Configurar Jackson:
  - [ ] `snake_case`.
  - [ ] `BigDecimal` como string.
  - [ ] Fechas ISO-8601.
  - [ ] No serializar campos nulos innecesarios (según contrato).
- [ ] Configurar springdoc/OpenAPI con seguridad Bearer.
- [ ] Configurar CORS parametrizable para futuras integraciones, sin conectar mobile.
- [ ] Añadir endpoint mínimo de verificación (`/actuator/health` y, si se decide, `/api/v1/system/info`).
- [ ] Pruebas:
  - [ ] Envelope de éxito.
  - [ ] Envelope de error.
  - [ ] Correlation ID generado, propagado y devuelto.
  - [ ] Validación de DTOs.

**Entregables**

- Formato de respuesta y error uniforme.
- OpenAPI disponible en dev.
- Filtros transversales funcionando.

**Criterio de salida**

- Todos los endpoints futuros pueden apoyarse en el envelope y el manejo global.
- Un error inesperado nunca expone stack trace ni SQL.

---

### F5 — Seguridad, JWT y multitenancy

**Tecnología/responsabilidad:** Spring Security, JWT, BCrypt, autorización, aislamiento.

**Tareas**

- [ ] Implementar entidad/repositorio de lectura de `User` y `Tenant` para autenticación.
- [ ] Configurar `PasswordEncoder` (BCrypt, strength 12; Argon2 queda como alternativa).
- [ ] Configurar `SecurityFilterChain`:
  - [ ] Stateless, CSRF deshabilitado.
  - [ ] Rutas públicas: `/api/v1/auth/login`, `/actuator/health`, OpenAPI/Swagger en `dev`.
  - [ ] Resto autenticado.
- [ ] Configurar JWT con Spring Security OAuth2 Resource Server + Nimbus:
  - [ ] Firma HS256 con `JWT_SECRET` de entorno (mínimo 32 bytes).
  - [ ] Claims: `sub` (user id), `tenant_id`, `roles`, `iat`, `exp`, `jti`.
  - [ ] Expiración configurable (`JWT_EXPIRATION_MS`, por defecto 1 hora).
  - [ ] Validar firma, expiración y presencia de `tenant_id`.
- [ ] Implementar `POST /api/v1/auth/login`:
  - [ ] Request: `tenant_code`, `email` (o `username`), `password`.
  - [ ] Validar tenant `ACTIVE` y usuario `ACTIVE`.
  - [ ] Error genérico `INVALID_CREDENTIALS` para no filtrar existencia.
  - [ ] Emitir token y respuesta estándar.
- [ ] Implementar `GET /api/v1/auth/me`.
- [ ] Implementar `TenantContext` a partir del JWT y asegurar limpieza al final del request.
- [ ] Aplicar autorización por rol con `@PreAuthorize` o reglas de `SecurityFilterChain`.
- [ ] Prohibir explícitamente el uso de `tenant_id` del body/path para autorización.
- [ ] Pruebas:
  - [ ] Login válido/inválido/tenant inactivo/usuario inactivo.
  - [ ] Token ausente, inválido, expirado y sin tenant → 401.
  - [ ] Rol insuficiente → 403.
  - [ ] Acceso cruzado entre tenants → 403/404 sin fuga.

**Entregables**

- Login y `/auth/me` funcionales.
- Contexto de seguridad y tenant disponible para servicios.
- Pruebas de seguridad iniciales.

**Criterio de salida**

- Un endpoint protegido no puede invocarse sin JWT válido.
- El tenant efectivo siempre proviene del token.
- No existe forma de operar sobre recursos de otro tenant mediante IDs manipulados.

---

### F6 — Módulos de dominio

> Orden recomendado: 6.1 → 6.2 → 6.3 → 6.4 → 6.5 → 6.6 → 6.7 → 6.8 → 6.9.
>
> Cada módulo debe incluir `controller`, `service`, `repository`, `entity`, `dto` y pruebas. No se expone la entidad JPA.

#### F6.1 — Tenants

- [ ] Entidad `Tenant` y repositorio.
- [ ] `POST /api/v1/tenants` (solo `PLATFORM_ADMIN`).
- [ ] `GET /api/v1/tenants/{id}` (según permisos).
- [ ] Validar `code` único, estado y datos obligatorios.
- [ ] Auditar creación.
- [ ] Pruebas de autorización y unicidad.

#### F6.2 — Users

- [ ] CRUD administrativo mínimo: crear, listar y obtener usuarios del tenant.
- [ ] Asignar rol del enum D5.
- [ ] Hashear contraseña; nunca devolver `password_hash`.
- [ ] Unicidad `(tenant_id, email)`.
- [ ] Solo `TENANT_ADMIN` puede administrar usuarios del tenant.
- [ ] Pruebas de aislamiento y validación.

#### F6.3 — Customers

- [ ] Crear, listar y obtener clientes del tenant.
- [ ] Campos: tipo/número de documento, nombre completo, estado.
- [ ] Unicidad `(tenant_id, document_number)`.
- [ ] Permisos: `TENANT_ADMIN`/`OPERATOR`; lectura según rol.
- [ ] Pruebas.

#### F6.4 — Accounts

- [ ] Crear cuenta asociada a un customer del mismo tenant.
- [ ] Generar `account_number` único por tenant.
- [ ] Moneda ISO 4217, estado `ACTIVE`/`INACTIVE`/`BLOCKED`, saldo inicial 0.
- [ ] Listar y obtener cuentas del tenant; filtros básicos.
- [ ] Nunca aceptar saldo inicial arbitrario desde el cliente (los saldos cambian por operaciones).
- [ ] Pruebas de pertenencia y estado.

#### F6.5 — Settings y exchange rates

- [ ] `GET`/`PUT /api/v1/settings` para configuración del tenant.
- [ ] `GET`/`POST /api/v1/settings/exchange-rates`.
- [ ] Modelo de tasas con vigencia (`effective_from`, `effective_to` o `active`).
- [ ] Comisiones del MVP: fija por operación y/o porcentual, configurables por tenant.
- [ ] Validar monedas y tasas positivas.
- [ ] Pruebas de vigencia y cálculo.

#### F6.6 — Idempotency

- [ ] Entidad `IdempotencyRecord` con estados `IN_PROGRESS`, `COMPLETED`, `FAILED`.
- [ ] Servicio genérico para ejecutar operaciones financieras idempotentes.
- [ ] Requerir `Idempotency-Key` en depósitos, retiros y transferencias; si falta → 400.
- [ ] Calcular `request_hash` canónico (SHA-256) del body + operación + tenant.
- [ ] Misma clave + mismo hash + `COMPLETED` → devolver respuesta previa.
- [ ] Misma clave + hash distinto → `409 IDEMPOTENCY_CONFLICT`.
- [ ] Misma clave en progreso → `409 IDEMPOTENCY_IN_PROGRESS` (o 425 si el equipo lo decide).
- [ ] Persistir snapshot de respuesta en PostgreSQL.
- [ ] Pruebas de replay, conflicto y concurrencia.

#### F6.7 — Transactions

- [ ] Entidades `Transaction` y `TransactionEntry` con estados y direcciones `DEBIT`/`CREDIT`.
- [ ] Implementar `POST /api/v1/transactions/deposits`:
  - [ ] Validar cuenta activa, moneda y monto > 0.
  - [ ] Bloquear cuenta, acreditar saldo, crear transacción y asiento.
- [ ] Implementar `POST /api/v1/transactions/withdrawals`:
  - [ ] Validar cuenta activa, saldo suficiente y monto > 0.
  - [ ] Bloquear cuenta, debitar saldo, crear transacción y asiento.
- [ ] Implementar `POST /api/v1/transactions/transfers`:
  - [ ] Validar cuentas del mismo tenant, distintas, activas.
  - [ ] Bloquear ambas cuentas en orden determinista por UUID.
  - [ ] Validar saldo y calcular comisión/conversión.
  - [ ] Debitar origen (monto + comisión), acreditar destino (monto convertido).
  - [ ] Crear transacción con `fee` y `exchange_rate` cuando aplique.
  - [ ] Crear dos asientos (`DEBIT` origen, `CREDIT` destino).
- [ ] Implementar `POST /api/v1/transactions/transfers/preview`:
  - [ ] Calcular comisión, tasa, monto debitado y acreditado sin escribir ni bloquear.
- [ ] Implementar `GET /api/v1/transactions` con paginación y filtros.
- [ ] Implementar `GET /api/v1/transactions/{id}` con asientos.
- [ ] Implementar `GET /api/v1/accounts/{id}/transactions`.
- [ ] Auditar cada operación exitosa y los rechazos relevantes.
- [ ] Registrar evento outbox después de confirmar la transacción.
- [ ] Pruebas unitarias de cálculos y pruebas de integración de cada flujo.

#### F6.8 — Audit

- [ ] Entidad `AuditLog` inmutable.
- [ ] Servicio `AuditService.record(...)` con actor, tenant, acción, recurso, resultado, metadata y `correlation_id`.
- [ ] Integrar auditoría en login, usuarios, cuentas y operaciones financieras.
- [ ] `GET /api/v1/audit-logs` y `GET /api/v1/audit-logs/{id}` para `TENANT_ADMIN`/`AUDITOR`.
- [ ] Sin endpoints de actualización o borrado.
- [ ] Pruebas de registro y consulta.

#### F6.9 — Webhooks y Outbox (P1, no bloquea MVP)

- [ ] `webhook_configs`: URL, eventos suscritos, secreto, estado.
- [ ] `outbox_events`: agregado, tipo de evento, payload JSONB, estado, intentos, `next_retry_at`.
- [ ] Persistir evento en la misma transacción de la operación financiera.
- [ ] Dispatcher programado con `FOR UPDATE SKIP LOCKED` para tomar eventos pendientes.
- [ ] Envío HTTP con firma HMAC y timeout.
- [ ] Reintentos con backoff exponencial y estado final `DELIVERED`/`DEAD`.
- [ ] `POST`/`GET /api/v1/webhooks` y `GET /api/v1/webhooks/events`.
- [ ] Pruebas: fallo externo no revierte la operación financiera; reintentos registrados.

---

### F7 — Endurecimiento de consistencia financiera

**Tecnología/responsabilidad:** transacciones SQL, concurrencia, precisión decimal.

**Tareas**

- [ ] Crear utilidad monetaria central (`MoneyUtils` o value object `Money`) con escala y redondeo explícitos.
- [ ] Prohibir `float`/`double` en código y revisar con búsqueda en CI.
- [ ] Implementar bloqueo pesimista (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`) en cuentas afectadas.
- [ ] Ordenar bloqueos por UUID para evitar deadlocks.
- [ ] Validar saldo y estado **después** de adquirir el bloqueo, dentro de la misma transacción.
- [ ] Definir aislamiento (`READ_COMMITTED` por defecto) y documentarlo.
- [ ] Verificar atomicidad: cualquier fallo revierte saldos, transacción, asientos, idempotencia y auditoría.
- [ ] Añadir consulta de reconciliación: saldo de cuenta vs suma de asientos.
- [ ] Probar concurrencia:
  - [ ] Dos retiros simultáneos sobre la misma cuenta no permiten doble gasto.
  - [ ] Dos transferencias cruzadas no generan deadlock.
  - [ ] Dos solicitudes con la misma idempotency key ejecutan una sola operación.
- [ ] Documentar política de redondeo y comisiones.

**Criterio de salida**

- Saldos reconstruibles desde `transaction_entries`.
- No hay saldos negativos en el MVP.
- Pruebas de concurrencia e idempotencia en verde.

---

### F8 — Pruebas y calidad

**Tecnología/responsabilidad:** JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers.

**Tareas**

- [ ] Configurar Testcontainers PostgreSQL reutilizable para integración.
- [ ] Configurar perfil `test` con Flyway y datos de prueba multi-tenant.
- [ ] Unitarias:
  - [ ] Cálculo de comisiones y conversión.
  - [ ] Redondeo y precisión.
  - [ ] Validaciones de DTOs.
  - [ ] Reglas de estado de cuentas y saldo.
- [ ] Integración:
  - [ ] Migraciones Flyway.
  - [ ] Repositorios filtrados por tenant.
  - [ ] Constraints e índices críticos.
  - [ ] Envelope de respuesta y manejo de errores.
- [ ] Seguridad:
  - [ ] 401 sin token, token inválido/expirado.
  - [ ] 403 por rol insuficiente.
  - [ ] 403/404 por acceso cruzado entre tenants.
  - [ ] `tenant_id` del body ignorado.
- [ ] Financieras:
  - [ ] Depósito, retiro y transferencia exitosos.
  - [ ] Saldo insuficiente sin efectos parciales.
  - [ ] Cuenta inactiva/bloqueada rechazada.
  - [ ] Transferencia multimoneda con comisión y tasa.
  - [ ] Idempotencia: replay, conflicto y concurrencia.
  - [ ] Concurrencia de retiros/transferencias.
- [ ] Contrato:
  - [ ] Formato `success/code/message/data/correlation_id`.
  - [ ] Códigos HTTP y `ErrorCode` estables.
- [ ] Configurar JaCoCo y umbral mínimo (sugerido: 70% líneas global, 80% en servicios financieros).
- [ ] Documentar comandos y evidencia en `backend/docs/quality/`.

**Criterio de salida**

- `./mvnw verify` en verde.
- Casos críticos de aceptación cubiertos.
- CI ejecuta la misma suite.

---

### F9 — Observabilidad y operación

**Tecnología/responsabilidad:** Spring Actuator, logging, configuración.

**Tareas**

- [ ] Exponer `/actuator/health` y `/actuator/info`; health con PostgreSQL y Redis.
- [ ] Configurar logs con `correlation_id`, `tenant_id`, `user_id` y endpoint, sin datos sensibles.
- [ ] Validar variables de entorno al arrancar (`@ConfigurationProperties` + `@Validated`).
- [ ] Diferenciar perfiles `dev`, `test`, `prod`.
- [ ] Documentar endpoints de salud y logs.
- [ ] P1: métricas Micrometer/Prometheus y rate limiting con Redis.
- [ ] P1: logs estructurados JSON.

**Criterio de salida**

- Un fallo de PostgreSQL/Redis se refleja en health.
- Los logs permiten seguir una operación por `correlation_id`.

---

### F10 — CI/CD

**Tecnología/responsabilidad:** GitHub Actions, Maven, Docker.

**Tareas**

- [ ] Crear `.github/workflows/backend-ci.yml`:
  - [ ] Trigger en PR y push a `develop`/`main`.
  - [ ] JDK 21 con `actions/setup-java`.
  - [ ] Cache Maven.
  - [ ] `./mvnw -B verify`.
  - [ ] Build de imagen Docker.
  - [ ] Validación de `docker compose config`.
- [ ] Publicar reporte de pruebas/cobertura como artefacto.
- [ ] Documentar branch protection: PR obligatorio, 1 aprobación, conversaciones resueltas.
- [ ] No incluir despliegue automático en esta fase.

**Criterio de salida**

- Un PR del backend ejecuta build, pruebas y build de imagen.
- No se requieren secretos reales para CI.

---

### F11 — Documentación y preparación para integración futura

**Tecnología/responsabilidad:** documentación técnica y contrato de integración.

**Tareas**

- [ ] Actualizar `backend/docs/api/api-contracts.md` con estado **implementado** y ejemplos reales.
- [ ] Publicar OpenAPI versionado (artefacto o endpoint en dev).
- [ ] Actualizar `backend/README.md` con:
  - [ ] Requisitos.
  - [ ] Comandos Docker y Maven.
  - [ ] Variables de entorno.
  - [ ] Credenciales seed de desarrollo.
  - [ ] URLs de salud y Swagger.
- [ ] Crear colección HTTP (Bruno/Postman/curl) con login y flujos MVP.
- [ ] Documentar catálogo final de `ErrorCode`.
- [ ] Documentar contrato de headers: `Authorization`, `X-Correlation-ID`, `Idempotency-Key`.
- [ ] Actualizar estado en `docs/README.md` y `backend/docs/README.md`.
- [ ] Crear checklist de integración futura (sección 10).
- [ ] **No modificar ni conectar `mobile/`.**

**Criterio de salida**

- Un desarrollador de frontend puede integrarse usando solo OpenAPI, README y datos seed.
- El backend sigue siendo independiente y verificable por sí solo.

---

## 7. Backlog resumido por endpoint

| Método | Ruta | Fase | Rol mínimo | Idempotency-Key | Prioridad |
|---|---|---|---|---|---|
| POST | `/api/v1/tenants` | F6.1 | PLATFORM_ADMIN | No | P0 |
| GET | `/api/v1/tenants/{id}` | F6.1 | PLATFORM_ADMIN/TENANT_ADMIN | No | P0 |
| POST | `/api/v1/auth/login` | F5 | Público | No | P0 |
| GET | `/api/v1/auth/me` | F5 | Autenticado | No | P0 |
| POST | `/api/v1/users` | F6.2 | TENANT_ADMIN | No | P0 |
| GET | `/api/v1/users` | F6.2 | TENANT_ADMIN | No | P0 |
| GET | `/api/v1/users/{id}` | F6.2 | TENANT_ADMIN | No | P0 |
| POST | `/api/v1/customers` | F6.3 | ADMIN/OPERATOR | No | P0 |
| GET | `/api/v1/customers` | F6.3 | ADMIN/OPERATOR | No | P0 |
| GET | `/api/v1/customers/{id}` | F6.3 | ADMIN/OPERATOR | No | P0 |
| POST | `/api/v1/accounts` | F6.4 | ADMIN/OPERATOR | No | P0 |
| GET | `/api/v1/accounts` | F6.4 | ADMIN/OPERATOR | No | P0 |
| GET | `/api/v1/accounts/{id}` | F6.4 | ADMIN/OPERATOR | No | P0 |
| GET | `/api/v1/accounts/{id}/transactions` | F6.7 | ADMIN/OPERATOR | No | P0 |
| POST | `/api/v1/transactions/deposits` | F6.7 | OPERATOR | Sí | P0 |
| POST | `/api/v1/transactions/withdrawals` | F6.7 | OPERATOR | Sí | P0 |
| POST | `/api/v1/transactions/transfers` | F6.7 | OPERATOR | Sí | P0 |
| POST | `/api/v1/transactions/transfers/preview` | F6.7 | OPERATOR | No | P0 |
| GET | `/api/v1/transactions` | F6.7 | ADMIN/OPERATOR | No | P0 |
| GET | `/api/v1/transactions/{id}` | F6.7 | ADMIN/OPERATOR | No | P0 |
| GET | `/api/v1/settings` | F6.5 | ADMIN | No | P1 |
| PUT | `/api/v1/settings` | F6.5 | TENANT_ADMIN | No | P1 |
| GET | `/api/v1/settings/exchange-rates` | F6.5 | ADMIN/OPERATOR | No | P1 |
| POST | `/api/v1/settings/exchange-rates` | F6.5 | TENANT_ADMIN | No | P1 |
| GET | `/api/v1/audit-logs` | F6.8 | ADMIN/AUDITOR | No | P1 |
| GET | `/api/v1/audit-logs/{id}` | F6.8 | ADMIN/AUDITOR | No | P1 |
| POST | `/api/v1/webhooks` | F6.9 | TENANT_ADMIN | No | P2 |
| GET | `/api/v1/webhooks` | F6.9 | TENANT_ADMIN | No | P2 |
| GET | `/api/v1/webhooks/events` | F6.9 | TENANT_ADMIN | No | P2 |

**Nota:** los roles exactos y la necesidad de `Idempotency-Key` pueden ajustarse en F0, pero deben quedar consistentes con la documentación.

---

## 8. Orden de ejecución y hitos

### Hito M0 — Base ejecutable (F0–F4)

- Decisiones y contratos congelados.
- Proyecto Maven compilando.
- Docker Compose con PostgreSQL/Redis.
- Migraciones Flyway aplicadas.
- Envelope, errores, correlation ID y OpenAPI.

**Resultado:** backend arranca, health responde y esquema existe. Aún no hay login.

### Hito M1 — Identidad y datos maestros (F5, F6.1–F6.4)

- Login JWT y `/auth/me`.
- Tenants, usuarios, clientes y cuentas.
- Aislamiento multitenant probado.

**Resultado:** se puede autenticar y administrar datos base del tenant.

### Hito M2 — Núcleo financiero (F6.6–F6.8, F7)

- Idempotencia.
- Depósitos, retiros, transferencias e historial.
- Auditoría.
- Bloqueos y pruebas de concurrencia.

**Resultado:** flujo bancario principal completo y consistente.

### Hito M3 — Configuración avanzada (F6.5, F6.9)

- Settings, comisiones y tasas.
- Webhooks/outbox y reintentos.

**Resultado:** operación multimoneda configurable y notificaciones externas.

### Hito M4 — Calidad y entrega (F8–F11)

- Suite completa.
- CI.
- Observabilidad.
- Documentación y preparación de integración.

**Resultado:** backend listo para ser consumido por el frontend en una fase futura.

### Ruta MVP para hackatón de 10 horas

Si el tiempo es limitado, ejecutar en este orden y recortar lo demás:

1. **F0** solo decisiones D1-D10 y contratos de login/cuentas/transacciones.
2. **F1** proyecto Maven mínimo.
3. **F2** PostgreSQL + Redis con Compose (sin imagen `app` si se corre con Maven).
4. **F3** migraciones base.
5. **F4** envelope + errores + correlation ID.
6. **F5** login JWT.
7. **F6.1–F6.4** seed y endpoints de lectura/creación.
8. **F6.7** depósito, retiro, transferencia misma moneda e historial.
9. **F6.6** idempotencia básica.
10. **F6.8** auditoría básica.
11. **F8** pruebas críticas.
12. **F11** README + OpenAPI + colección HTTP.

**Recortes explícitos para el MVP:** conversión multimoneda, webhooks/outbox, refresh tokens, rate limiting, métricas, RBAC con tablas, doble partida estricta y CI completo.

---

## 9. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Java/Maven no instalados en el entorno | Bloquea F1 | Instalar JDK 21 o generar/build con contenedor Maven. |
| Alcance demasiado grande para 10 horas | Entrega incompleta | Ejecutar ruta MVP y aplazar P1/P2. |
| Errores de precisión monetaria | Inconsistencia financiera | `BigDecimal`/`NUMERIC`, escala explícita, pruebas de redondeo. |
| Fuga de datos entre tenants | Crítico de seguridad | `TenantContext`, filtros explícitos, pruebas de acceso cruzado. |
| Idempotencia mal implementada | Doble ejecución de operaciones | Restricción única, request hash, estados y pruebas concurrentes. |
| Deadlocks en transferencias | Errores 500 intermitentes | Bloqueo ordenado por UUID, reintentos controlados, pruebas. |
| Conversión/comisión ambigua | Saldos incorrectos | Congelar reglas en F0 y probar casos multimoneda. |
| Webhooks bloquean operaciones | Transacciones confirmadas revertidas | Outbox en misma transacción y envío asíncrono. |
| Secretos en repositorio | Riesgo de seguridad | `.env.example`, `.gitignore`, revisión en PR. |
| Testcontainers no corre en CI | Pruebas de integración ausentes | Verificar Docker en runner y cachear imágenes. |
| Documentación duplicada (`docs/` y `backend/docs/`) | Contratos divergentes | Mantener `backend/docs/` como fuente del backend y enlazar desde `docs/`. |

---

## 10. Checklist para dejar el backend listo para integración futura

> Esta lista define cuándo el backend está preparado para que otro equipo conecte Flutter u otro cliente.

- [x] API estable bajo `/api/v1` y OpenAPI publicado.
- [x] Contrato de login documentado (`tenant_code`, credenciales, token, expiración).
- [x] Catálogo de `ErrorCode` y estructura de error estable.
- [x] Headers documentados: `Authorization`, `X-Correlation-ID`, `Idempotency-Key`.
- [x] CORS configurable por variable de entorno.
- [x] Docker Compose levanta backend + PostgreSQL + Redis con un comando.
- [x] Datos seed reproducibles para desarrollo.
- [x] Credenciales de prueba documentadas (solo dev).
- [ ] Colección HTTP con flujos de login, cuentas, depósito, retiro y transferencia.
- [x] Pruebas base en verde (unitarias e integración).
- [ ] Pruebas de concurrencia, contrato y carga.
- [x] `README` con URLs, comandos y solución de problemas.
- [x] Versionado de API y política de cambios incompatibles documentada.
- [x] Confirmación explícita de que `mobile/` no fue modificado.

---

## 11. Próximos pasos inmediatos

1. Agregar Maven Wrapper y workflow de CI (build + tests + imagen).
2. Implementar refresh tokens, rotación de secretos y revocación de sesiones.
3. Agregar rate limiting y bloqueo por intentos fallidos.
4. Evaluar RLS de PostgreSQL y claves Redis con prefijo de tenant.
5. Agregar pruebas de concurrencia, contrato y carga.
6. Crear una colección HTTP reproducible (Bruno/Postman/curl).
7. Completar reconciliación contable, cierre diario y reportes.
8. En una fase futura y separada, integrar `mobile/` con el contrato ya publicado.

---

## 12. Definition of Done global del backend

Un entregable backend se considera terminado cuando:

- [ ] Compila y pasa `./mvnw verify`.
- [ ] Respeta la arquitectura modular y las capas.
- [ ] No expone entidades JPA ni datos sensibles.
- [ ] Usa `BigDecimal`/`NUMERIC` para dinero.
- [ ] Valida tenant desde el contexto autenticado.
- [ ] Tiene pruebas unitarias y/o de integración relevantes.
- [ ] Actualiza contratos/documentación cuando cambia la API.
- [ ] No modifica ni requiere `mobile/`.
- [ ] No incluye secretos reales.
- [ ] Cuenta con evidencia de ejecución en el PR.
