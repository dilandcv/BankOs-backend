# Guía de inicio (Getting Started)

## Estado (auditado 2026-11-12)
- **Implementado:** ninguno. El backend no cuenta con proyecto Maven, código, configuración, migraciones ni contenedores.
- **Ejecutable/verificable de forma independiente hoy:** **no** (no hay artefactos para compilar ni ejecutar).
- **Objetivo:** permitir levantar y probar el backend por sí solo (API + PostgreSQL + Redis) sin depender del frontend.

## Prerrequisitos (propuestos para cuando exista implementación)

| Herramienta | Versión objetivo | Propósito | Estado |
|---|---|---|---|
| Java | 21 (LTS) | Runtime/compilación | Pendiente de adopción en proyecto |
| Maven | 3.9+ (o wrapper) | Build y dependencias | No implementado |
| Docker / Docker Compose | Recientes | PostgreSQL, Redis y app | No implementado |
| Git | Cualquiera soportado | Control de versiones | Presente en repo |

## Estructura esperada (propuesta)

```text
backend/
├── src/main/java/com/bankos/...
├── src/main/resources/application.yml
├── src/main/resources/db/migration/
├── src/test/java/...
├── pom.xml
├── Dockerfile
└── docker-compose.yml
```

Actualmente `backend/` está vacío.

## Configuración (propuesta)

- Externalizar configuración por **variables de entorno** y perfiles (`dev`, `test`, `prod`).
- **No commitear secretos reales** (credenciales, llaves JWT, tokens, secretos de webhooks).
- Proveer un `.env.example` (sin valores sensibles) cuando se implemente la infraestructura.

Ejemplo ilustrativo (no implementado):

```yaml
# application.yml (ejemplo propuesto)
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}

security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION}
```

## Ejecución local (objetivo, pendiente de implementación)

### Con Docker Compose (recomendado, propuesto)
1. Definir `docker-compose.yml` con servicios `app` (Spring Boot), `postgres` y `redis`.
2. Crear `.env` local a partir de `.env.example` (sin secretos en git).
3. Ejecutar: `docker compose up --build`.
4. Verificar healthchecks y disponibilidad de la API (p. ej. endpoint de salud, cuando exista).

### Con Maven (propuesto)
1. Levantar PostgreSQL y Redis (locales o vía Compose).
2. Exportar variables de entorno requeridas.
3. Ejecutar: `./mvnw spring-boot:run` (o `mvn spring-boot:run`).
4. Ejecutar pruebas: `./mvnw test` / `./mvnw verify`.

> Los comandos anteriores son **referencia futura**; no son ejecutables en el estado actual del repositorio.

## Pruebas manuales independientes (propuestas)
- Usar herramientas HTTP (curl, HTTPie, Postman/Insomnia) contra `/api/v1`.
- Incluir encabezados `Authorization: Bearer <JWT>`, `Content-Type: application/json`, `X-Correlation-ID` e `Idempotency-Key` en operaciones financieras.
- No se requiere ni debe usarse la aplicación móvil para validar el backend.

## Problemas conocidos actuales
- No hay build, dependencias ni punto de entrada de aplicación.
- No hay base de datos ni migraciones.
- No hay contenedores ni variables de entorno de referencia.
- Toda verificación funcional está **pendiente** hasta la implementación inicial.