# Definition of Done (DoD)

## Estado
- **Implementado:** no aplicable (no hay funcionalidades backend implementadas ni flujo de entrega verificable).
- **Propuesto:** criterios para considerar terminado un trabajo de backend, alineados con independencia del frontend, seguridad financiera y calidad.

## DoD general (propuesto)

Un ítem (historia, tarea o fix) se considera **Done** cuando:

1. **Alcance y diseño**
   - Cumple los criterios de aceptación acordados.
   - Respeta la arquitectura de monolito modular y la separación por capas.
   - No introduce dependencias circulares ni acoplamiento indebido entre módulos.

2. **Implementación**
   - Código compila y pasa verificaciones locales/CI definidas (cuando existan).
   - Validaciones de entrada (Jakarta Validation) y manejo de errores implementados según estándar.
   - No expone entidades JPA en la API; usa DTOs.
   - No incluye secretos, credenciales ni datos sensibles hardcodeados.

3. **Seguridad y multitenancy**
   - Endpoints protegidos según autenticación/autorización requerida.
   - `tenant_id` resuelto desde contexto autenticado; pertenencia de recursos validada.
   - Sin posibilidad de acceso cruzado entre tenants para el caso implementado.

4. **Seguridad financiera (cuando aplique)**
   - Importes manejados con `BigDecimal`/`NUMERIC`.
   - Operaciones atómicas y con control de concurrencia adecuado.
   - Idempotencia implementada y persistida correctamente (PostgreSQL fuente de verdad).
   - Movimientos financieros no eliminados físicamente; auditoría registrada.
   - Reglas de comisión/conversión y estados de cuenta respetadas.

5. **Pruebas**
   - Pruebas unitarias y/o de integración relevantes añadidas o actualizadas.
   - Casos críticos cubiertos (errores de negocio, seguridad, tenant, concurrencia/idempotencia según corresponda).
   - Evidencia de ejecución (comando/resultado) incluida en el PR.

6. **Documentación**
   - Contratos API, configuración y decisiones técnicas documentadas/actualizadas en `backend/docs/` cuando el cambio lo requiera.
   - Se distingue lo implementado de lo propuesto/pendiente.

7. **Revisión e integración**
   - Pull Request hacia `develop` con descripción completa y checklist.
   - Revisado y aprobado por al menos otro integrante (sin autoaprobación).
   - Conversaciones resueltas y sin cambios fuera de alcance.

## DoD específico por tipo de cambio

### Endpoint/API
- Ruta, método, request/response, códigos HTTP y errores documentados.
- `X-Correlation-ID` y formato estándar de respuesta/error aplicados.
- `Idempotency-Key` exigido en operaciones financieras.

### Cambio de base de datos
- Migración Flyway versionada y reversible según estrategia acordada (si aplica).
- Restricciones, índices e integridad referencial considerados.
- Compatible con aislamiento por `tenant_id`.

### Infraestructura/Docker
- Servicios levantan de forma reproducible; healthchecks y variables documentadas.
- Sin secretos en repositorio; `.env.example` actualizado si corresponde.
- Backend verificable de forma independiente.

## Criterios de exclusión (no es Done)
- Funciona solo con suposiciones no validadas o con datos hardcodeados inseguros.
- Requiere conexión o cambios en `mobile/` para ser validado (en esta fase).
- Carece de pruebas en lógica financiera/seguridad crítica afectada.
- Permite acceso sin tenant o acceso cruzado entre tenants.
- Introduce `float`/`double` para dinero o elimina movimientos financieros.

## Estado actual del backend respecto al DoD
- **No hay entregables de backend que puedan evaluarse como Done.** La base de proyecto, módulos, API, seguridad, persistencia, infraestructura y pruebas están **pendientes**.