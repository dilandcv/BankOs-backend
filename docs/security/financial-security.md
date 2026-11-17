# Seguridad financiera

## Estado

- **Implementado:** `BigDecimal`/`NUMERIC(19,4)`, bloqueo pesimista, idempotencia, auditoría, outbox y movimientos contables inmutables.
- **Pendiente:** conciliación contable, doble partida estricta con cuentas de sistema, pruebas de concurrencia/carga y alertas.

## Reglas implementadas

| Regla | Implementación | Estado |
|---|---|---|
| Precisión monetaria | `BigDecimal` escala 4, `NUMERIC(19,4)`, `RoundingMode.HALF_UP` | Implementado |
| Tasas de cambio | `NUMERIC(19,8)` | Implementado |
| Atomicidad | `@Transactional` en servicios financieros | Implementado |
| Concurrencia | `PESSIMISTIC_WRITE` sobre cuentas | Implementado |
| Orden de bloqueo | Cuentas ordenadas por UUID en transferencias | Implementado |
| Sin sobregiro | `CHECK (balance >= 0)` + validación de saldo | Implementado |
| Idempotencia | Único `(tenant_id, operation, key)`, hash y snapshot | Implementado |
| Replay/conflicto | Misma llave/cuerpo → replay; distinto → `409` | Implementado |
| Movimientos contables | `transaction_entries` por cuenta | Implementado |
| Inmutabilidad | Sin borrado físico de movimientos | Implementado |
| Auditoría | `audit_logs` en la misma transacción | Implementado |
| Webhooks | Outbox posterior al commit | Implementado (MVP) |

## Operaciones

- **Depósito:** valida cuenta activa, moneda y saldo resultante; acredita y crea entry.
- **Retiro:** valida saldo suficiente y estado; debita y crea entry.
- **Transferencia:** bloquea origen y destino, valida moneda/tasa, calcula comisión, debita/acredita y crea entries.
- **Preview:** calcula tasa y comisión sin persistir.

## Idempotencia

1. `Idempotency-Key` obligatorio.
2. Se calcula hash del cuerpo.
3. Se toma advisory lock por tenant + operación + llave.
4. Si existe un registro completado:
   - mismo hash → devuelve snapshot;
   - hash distinto → `409 IDEMPOTENCY_CONFLICT`.
5. Si no existe, se ejecuta la operación y se guarda snapshot en la misma transacción.

## Auditoría

Se registra actor, tenant, acción, entidad, payload y correlation ID. La auditoría no reemplaza los movimientos contables.

## Webhooks

- Evento outbox en la misma transacción.
- Dispatcher programado envía después del commit.
- Reintentos con estado e intentos.
- Un fallo de webhook no revierte la operación financiera.

## Pendiente

- Conciliación diaria y cierre contable.
- Cuenta de sistema para comisiones.
- Detección de anomalías y alertas de saldo.
- Pruebas de concurrencia y carga.
- Worker de webhooks separado.
