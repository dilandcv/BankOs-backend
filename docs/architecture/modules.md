# Módulos del backend

## Estado general
- **Implementado:** ninguno.
- **Parcial/Propuesto/Pendiente:** todos los módulos funcionales y transversales descritos a continuación son **propuestos y pendientes** de implementación.

## Organización por capas (propuesta)

Cada módulo puede contener:

```text
module/
├── controller/   # HTTP: entrada/salida, validación de formato, delegación
├── service/      # Reglas de negocio y orquestación transaccional
├── repository/   # Acceso a datos (Spring Data JPA), consultas acotadas por tenant
├── entity/       # Modelo persistente (JPA)
└── dto/          # Contratos de entrada/salida (no exponer entidades)
```

**Reglas:**
- `controller`: sin lógica de negocio/financiera.
- `service`: aplica reglas, permisos de negocio, atomicidad y consistencia.
- `repository`: sin lógica de negocio; filtra por `tenant_id` según contexto.
- `entity`: no se expone en la API.
- `dto`: valida entrada (Jakarta Validation) y define salida.

## Módulos funcionales

| # | Módulo | Propósito (propuesto) | Dependencias esperadas (propuesta) | Estado |
|---|---|---|---|---|
| 1 | **tenant** | Registro y administración de instituciones; base del aislamiento de datos. | `common/tenancy` | Pendiente |
| 2 | **authentication** | Inicio de sesión, validación de credenciales, emisión/validación de JWT. | `tenant`, `users`, `common/security` | Pendiente |
| 3 | **users** | Administración de usuarios, roles y permisos por tenant. | `tenant`, `common/security` | Pendiente |
| 4 | **customers** | Registro y gestión de clientes de la institución. | `tenant` | Pendiente |
| 5 | **accounts** | Cuentas, saldos, estados y monedas; consultas por tenant. | `tenant`, `customers` | Pendiente |
| 6 | **transactions** | Depósitos, retiros, transferencias, preview e historial; reglas financieras. | `accounts`, `idempotency`, `settings`, `audit`, `tenant` | Pendiente |
| 7 | **idempotency** | Prevención de operaciones duplicadas (`Idempotency-Key`, resultado persistido). | `tenant` (alcance) | Pendiente |
| 8 | **settings** | Configuraciones por tenant: comisiones, tasas de cambio y parámetros operativos. | `tenant` | Pendiente |
| 9 | **audit** | Trazabilidad de operaciones y acciones relevantes. | `tenant`, `users`/contexto | Pendiente |
| 10 | **webhooks** | Notificaciones a servicios externos, reintentos y registro de entregas (patrón Outbox). | `tenant`, `audit`/eventos | Pendiente |

## Módulos/componentes transversales (propuestos)

| Componente | Propósito | Estado |
|---|---|---|
| `common/config` | Configuración de Spring, Jackson, Redis, seguridad, etc. | Pendiente |
| `common/security` | Filtros JWT, utilidades de autenticación/autorización, extracción de contexto. | Pendiente |
| `common/tenancy` | Resolución y propagación del tenant desde contexto autenticado; garantías de aislamiento. | Pendiente |
| `common/exception` | Excepciones de negocio/técnicas y manejo global (`@ControllerAdvice`). | Pendiente |
| `common/response` | Envoltorio estándar de respuestas y metadatos (`correlation_id`, códigos). | Pendiente |

## Responsabilidades clave por dominio (propuestas)

### Tenant
- Alta y gestión de instituciones.
- Asegurar que entidades de negocio estén asociadas a un `tenant_id`.
- **Regla:** nunca confiar en `tenant_id` provisto libremente por el cliente.

### Authentication y Users
- Login con credenciales; JWT con usuario, tenant, roles y expiración.
- Hashing seguro de contraseñas (p. ej. BCrypt/Argon2, decisión de implementación pendiente).
- Autorización por roles y protección de endpoints.

### Accounts y Transactions
- Saldos y movimientos con `BigDecimal`; estados de cuenta que impidan operaciones no permitidas.
- Transferencias: validación de pertenencia al tenant, estados, saldo, comisión, conversión (si aplica), ejecución atómica y registro.
- Sin eliminación física de movimientos financieros.

### Idempotency
- Clave única por operación; persistencia en PostgreSQL como fuente de verdad.
- Manejo de repeticiones, colisiones (misma clave, contenido distinto), concurrencia y reintentos.

### Settings
- Comisiones y tasas de cambio **por tenant**.
- Parametrización consumida por transacciones (reglas de vigencia/versión pendientes de definir).

### Audit y Webhooks
- Auditoría de acciones críticas y operaciones financieras.
- **Outbox:** registrar evento en la misma transacción de negocio y entregar/reintentar de forma asíncrona para no revertir lo confirmado por fallos externos.

## Límites y comunicación (propuestas)
- Dependencias unidireccionales y explícitas; **prohibidas las dependencias circulares**.
- Colaboración preferente vía servicios/interfaces de aplicación; eventos internos (Spring) opcionales a definir.
- `transactions` puede orquestar `accounts`, `idempotency`, `settings` y `audit`; los módulos no deben invertir dependencias sin contrato claro.

## Estado de verificación
No se encontraron paquetes, clases ni pruebas para ningún módulo; toda la definición anterior es **pendiente de implementación y validación**.