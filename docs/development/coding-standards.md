# Estándares de codificación

## Estado
- **Implementado:** no verificable (no existe código backend).
- **Propuesto:** convenciones obligatorias para Java/Spring Boot, PostgreSQL y Git a aplicar desde la implementación inicial.

## Java y Spring Boot

### Generales
- **Java 21** y **Spring Boot 3.5.x** (stack obligatorio).
- Código claro, nombres descriptivos y paquetes alineados a la arquitectura modular (`common/*`, `modules/*`).
- Evitar dependencias circulares entre módulos; respetar límites y responsabilidades.
- Preferir inyección por constructor; evitar lógica en constructores.

### Capas y responsabilidades
- **Controller:** solo HTTP (validación de formato, mapeo DTO, delegación). Sin reglas de negocio/financieras.
- **Service:** casos de uso, reglas de negocio, transacciones (`@Transactional`) y orquestación.
- **Repository:** acceso a datos y consultas; sin lógica de negocio; filtrar por `tenant`.
- **Entity:** modelo JPA; **no exponer** en la API.
- **DTO:** contratos de entrada/salida y validaciones (`jakarta.validation`).

### Validación y errores
- Usar **Jakarta Bean Validation** en DTOs (`@NotNull`, `@NotBlank`, `@Positive`, etc.).
- Centralizar manejo de excepciones (`@ControllerAdvice`) y respuestas de error consistentes con `correlation_id`.
- No filtrar detalles sensibles en mensajes de error.

### Dinero y precisión
- **`BigDecimal`** para todos los importes/cálculos monetarios; **prohibido** `float`/`double`.
- Definir escala y modo de redondeo explícitos (`RoundingMode`) según política acordada (pendiente).
- No usar tipos de punto flotante en comparaciones de saldo.

### Seguridad y multitenancy
- Obtener `tenant` y usuario desde el **contexto autenticado** (SecurityContext/JWT), nunca de entrada no confiable.
- Validar pertenencia de recursos al tenant y autorización por roles en backend.
- No registrar secretos, tokens o datos sensibles en logs.

### Persistencia
- Usar **Spring Data JPA/Hibernate** con consultas seguras (parámetros enlazados).
- Evitar N+1; definir fetch/consultas según necesidad.
- Operaciones financieras con control de concurrencia (bloqueos/estrategia definida) y atomicidad.

### Ejemplo ilustrativo (propuesto)

```java
// Ejemplo de servicio (propuesto, no implementado)
@Transactional
public TransferResult transfer(UUID tenantId, TransferCommand cmd) {
    Account source = accountRepository.findByIdAndTenantIdForUpdate(cmd.sourceId(), tenantId)
        .orElseThrow(() -> new NotFoundException("ACCOUNT_NOT_FOUND"));
    // validar estado, saldo (BigDecimal), comisión, ejecutar y auditar
    return result;
}
```

## PostgreSQL y migraciones

- Tipos monetarios: **`NUMERIC(p,s)`** (precisión/escala a confirmar).
- Claves primarias/foráneas, `NOT NULL`, `UNIQUE`, `CHECK` e índices apropiados (incl. por `tenant_id`).
- Migraciones con **Flyway** en `src/main/resources/db/migration/`, versionadas (`V__`), inmutables una vez aplicadas.
- No usar `ddl-auto=create/update` en entornos compartidos; preferir `validate` con esquema gestionado por Flyway.

## Pruebas (convenciones propuestas)
- Nombres descriptivos por comportamiento/caso.
- Pruebas unitarias para cálculos (comisiones, conversión, validaciones) y servicios.
- Pruebas de integración para repositorios, seguridad, Flyway, PostgreSQL/Redis.
- Casos obligatorios: concurrencia, idempotencia, aislamiento entre tenants y errores financieros.

## Git y commits

### Ramas
- `main`: estable.
- `develop`: integración.
- `feature/*`: funcionalidades.
- `fix/*`: correcciones.
- `refactor/*`: refactorizaciones.
- `docs/*`: documentación.
- `test/*`: pruebas.

**Nadie trabaja directamente sobre `main` o `develop`.** Cambios vía Pull Request hacia `develop`, revisados por otro integrante.

### Conventional Commits
- `feat:` nueva funcionalidad.
- `fix:` corrección.
- `refactor:` reestructuración sin cambio funcional.
- `docs:` documentación.
- `test:` pruebas.
- `chore:` tareas de mantenimiento.

Ejemplos: `feat: add transfer service with tenant validation`, `docs: document backend idempotency strategy`.

## Revisión y calidad (propuesto)
- PRs pequeños y enfocados; descripción clara del cambio y evidencia de pruebas.
- Verificar cumplimiento de seguridad financiera y multitenancy antes de aprobar.
- No introducir secretos ni configuraciones dependientes del frontend.