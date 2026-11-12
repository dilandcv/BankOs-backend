# Backend BankOS — Documentación

> **Estado auditado (2026-11-12):** el backend **no está implementado**. El directorio `backend/` se encuentra vacío (sin `pom.xml`, código fuente Java, configuración Spring, migraciones Flyway, Dockerfile, `docker-compose`, ni pruebas).  
> **Esta documentación** describe el estado real observado y el **diseño/contratos propuestos** para el desarrollo independiente del backend. No implica integración con el frontend.

## Alcance y restricciones

- **Backend independiente:** ejecutable y verificable por sí mismo (sin Flutter, sin clientes HTTP móviles, sin configuración compartida con `mobile/`).
- **Sin integración frontend-backend en esta fase:** no se modificó ni conectó `mobile/`.
- **Fuente de verdad técnica:** lo aquí descrito debe contrastarse con el código/configuración cuando existan; actualmente todo lo funcional es **propuesto/pendiente**.

## Convenciones de estado usadas en esta documentación

| Estado | Significado |
|---|---|
| **Implementado** | Verificado en el repositorio/código/configuración. |
| **Parcialmente implementado** | Existe parte, pero incompleto o no cumple el objetivo. |
| **Propuesto** | Diseño/contrato objetivo definido para implementación futura. |
| **Pendiente** | Requerido pero aún no definido o no iniciado. |
| **Inconsistente** | Evidencia contradictoria en el repositorio. |
| **No verificable** | No hay evidencia suficiente para confirmarlo (p. ej. sin runtime/código). |

## Índice

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

## Resumen del diagnóstico (auditoría 2026-11-12)

| Componente | Estado |
|---|---|
| Proyecto Maven / Spring Boot 3.5.x / Java 21 | No implementado |
| Módulos de dominio y capas (controller/service/repository/entity/dto) | No implementado |
| Spring Security / JWT / RBAC | No implementado |
| Multitenancy (`tenant_id`, aislamiento) | No implementado |
| PostgreSQL / Flyway / esquema | No implementado |
| Redis | No implementado |
| Docker / Docker Compose | No implementado |
| API REST `/api/v1` | Propuesta (no implementada) |
| Idempotencia, auditoría, webhooks/outbox | No implementado |
| Pruebas | No implementado |

## Decisiones pendientes de confirmación del equipo

- Precisión/escala monetaria, redondeo y política de saldos negativos (descubierto).
- Versionado/origen de tasas de cambio y reglas de comisiones por operación/tenant.
- Semántica exacta de reintentos e `Idempotency-Key` con payload distinto.
- Convenciones finales de códigos de negocio, paginación y catálogo de errores.
- Relación entre esta documentación (`backend/docs/`) y `docs/` de la raíz del repositorio.
