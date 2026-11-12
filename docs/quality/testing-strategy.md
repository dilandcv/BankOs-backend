# Estrategia de pruebas

## Estado
- **Implementado:** ninguno. No existe estructura de pruebas (`src/test`), dependencias de testing ni suites en el repositorio.
- **Propuesto:** estrategia por capas y por riesgos (seguridad, multitenancy y consistencia financiera) para el backend independiente.

## Objetivos
- Validar reglas de negocio y cálculos financieros con precisión (`BigDecimal`).
- Garantizar aislamiento multitenant y controles de seguridad (JWT/roles).
- Verificar consistencia transaccional, concurrencia e idempotencia.
- Asegurar regresión controlada en API, persistencia e infraestructura (PostgreSQL/Redis/Flyway).

## Pirámide y tipos de pruebas (propuestas)

| Tipo | Alcance | Herramientas sugeridas (propuestas) | Estado |
|---|---|---|---|
| Unitarias | Servicios, validaciones, cálculos, mapeos | JUnit 5, Mockito, AssertJ | Pendiente |
| Integración | Repositorios, JPA, Flyway, contexto Spring | Spring Boot Test, Testcontainers (PostgreSQL/Redis) | Pendiente |
| Seguridad | Autenticación, autorización, filtros JWT | Spring Security Test, MockMvc/WebTestClient | Pendiente |
| API/contrato | Controladores, validación, formatos de respuesta | MockMvc/WebTestClient, validación de esquemas (opcional) | Pendiente |
| Financieras/end-to-end de módulo | Flujos de depósito/retiro/transferencia | Spring Boot Test + BD real (contenedor) | Pendiente |

## Cobertura funcional requerida

### 1. Pruebas unitarias
- **Servicios:** reglas de negocio, estados permitidos, orquestación.
- **Validaciones:** DTOs y restricciones de entrada.
- **Cálculos financieros:** comisiones, conversión de moneda, redondeo (según política), totales debitados/acreditados.
- **Casos borde:** montos mínimos, precisión decimal, valores nulos/inválidos.

### 2. Pruebas de integración
- **Repositorios/JPA:** consultas por `tenant_id`, restricciones, unicidad e integridad referencial.
- **PostgreSQL:** migraciones Flyway, tipos `NUMERIC`, transacciones y bloqueos.
- **Redis:** integración de caché/soporte auxiliar (cuando aplique), sin sustituir fuente de verdad financiera.
- **Spring Security/JWT:** carga de contexto, filtros y propagación de identidad/tenant.
- **Manejo de errores:** `@ControllerAdvice`, códigos HTTP y formato estándar.

### 3. Pruebas de seguridad
- Acceso sin autenticación a endpoints protegidos → 401.
- Token inválido/expirado/mal formado → 401.
- Usuario sin rol/permiso suficiente → 403.
- **Acceso cruzado entre tenants:** lectura/modificación de recursos de otro tenant → 403/404 (sin fuga de información).
- Manipulación de identificadores en path/body para acceder a recursos ajenos.
- Validación de que el `tenant` efectivo proviene del contexto autenticado.

### 4. Pruebas financieras y de consistencia
- **Saldo insuficiente:** rechazo sin efectos parciales.
- **Depósitos y retiros:** actualización correcta de saldos y registro de transacción.
- **Transferencias:** débito/crédito atómicos, comisión en origen, conversión aplicada al monto transferido (no a la comisión), tasas por tenant.
- **Cuentas no operables:** bloqueo/inactividad impide operaciones según política.
- **Operaciones simultáneas:** concurrencia sobre misma cuenta (evitar doble gasto, mantener consistencia).
- **Idempotencia:** misma clave + mismo payload retorna mismo resultado; misma clave + payload distinto genera conflicto; solicitudes simultáneas manejadas correctamente.
- **Reintentos:** comportamiento seguro según resultado persistido.
- **No eliminación física:** movimientos/transacciones no se borran físicamente.

### 5. Pruebas de webhooks/outbox (propuestas)
- Evento outbox creado en la misma transacción de negocio.
- Fallos de entrega externa no revierten operación financiera confirmada.
- Reintentos y registro de estado/intentos de entrega.

## Datos y entornos de prueba (propuestos)
- Aislamiento por prueba (transacciones rollback o bases efímeras).
- Fixtures con múltiples tenants para validar aislamiento.
- Conjuntos de datos para multimoneda, comisiones y tasas vigentes.
- No usar secretos reales; configuración de prueba dedicada.

## Criterios mínimos de aceptación de calidad (propuestos)
- Suites unitarias e integración ejecutables en CI/local de forma independiente.
- Casos críticos financieros y de seguridad cubiertos y en verde.
- Sin regresiones en contratos de respuesta/error (`correlation_id`, códigos, estructura).
- Evidencia de ejecución en Pull Requests (cuando aplique).

## Riesgos actuales (por ausencia de pruebas)
- **Alto:** sin implementación ni pruebas no hay garantía de corrección financiera, aislamiento multitenant ni seguridad.
- La verificación del comportamiento descrito en esta documentación es **no verificable** hasta contar con código y suites automatizadas.