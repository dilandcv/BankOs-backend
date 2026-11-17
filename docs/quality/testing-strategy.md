# Estrategia de pruebas

## Estado

- **Implementado:** pruebas unitarias y de integración con JUnit 5, AssertJ, Spring Boot Test y Testcontainers (PostgreSQL).
- **Total actual:** 3 unitarias + 4 de integración.
- **Pendiente:** concurrencia, contrato, carga, webhooks y E2E con frontend.

## Pruebas actuales

| Clase | Cobertura |
|---|---|
| `MoneyUtilsTest` | Escala monetaria, redondeo y comparaciones. |
| `TransactionApiIntegrationTest` | Login, `/auth/me`, depósito, replay idempotente, conflicto, historial con filtros opcionales y aislamiento entre tenants. |

## Comando

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B test
```

Testcontainers `1.21.4` es necesario para compatibilidad con Docker Engine 29+.

## Estrategia por capas

### Unitarias
- Cálculos monetarios, redondeo, comisiones y tasas.
- Reglas puras de parsing/validación.

### Integración
- Contexto Spring completo.
- PostgreSQL real vía Testcontainers.
- Flyway desde cero.
- Seguridad JWT, RBAC y multitenancy.
- Endpoints financieros e idempotencia.

### Pendientes
- Concurrencia de depósitos/transferencias con la misma llave.
- Transferencias simultáneas en direcciones opuestas.
- Contrato contra OpenAPI.
- Carga y estrés.
- Outbox y reintentos de webhooks.
- Pruebas E2E cuando se integre `mobile/`.

## Calidad

- `mvn -B test` debe pasar antes de integrar.
- No se aceptan pruebas que dependan de datos productivos.
- Los tests de integración requieren Docker accesible.
- Cobertura mínima y análisis estático: pendiente de CI.
