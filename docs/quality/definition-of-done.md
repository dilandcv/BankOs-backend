# Definition of Done (DoD)

## Estado

- **Aplicable:** backend MVP implementado.
- **Pendiente:** automatización en CI y criterios de producción.

## Backend

- [x] Compila con Java 21 / Maven.
- [x] `mvn -B test` pasa (3 unitarias + 4 de integración).
- [x] Migraciones Flyway aplican desde cero.
- [x] Docker Compose levanta backend, PostgreSQL y Redis.
- [x] `/actuator/health` responde `UP`.
- [x] Login por institución y `/auth/me`.
- [x] Aislamiento por tenant y acceso cruzado `404`.
- [x] Depósito, retiro, transferencia y preview.
- [x] Idempotencia con replay y conflicto.
- [x] Historial con filtros opcionales.
- [x] Auditoría y outbox.
- [x] Swagger/OpenAPI y manejo de errores.
- [x] Documentación actualizada.
- [x] `mobile/` sin cambios.

## Criterios por historia

1. **Alcance:** cumple criterios de aceptación.
2. **Diseño:** respeta módulos, capas y aislamiento por tenant.
3. **Seguridad:** roles y tenant desde JWT; sin secretos en logs.
4. **Dinero:** `BigDecimal`, escala 4, sin `float`/`double`.
5. **Consistencia:** transacción atómica y auditoría.
6. **Idempotencia:** operaciones financieras con llave y manejo de conflicto.
7. **Pruebas:** unitarias/integración relevantes en verde.
8. **Documentación:** contratos y comportamiento actualizados.
9. **Frontend:** `mobile/` no se rompe ni se modifica sin alcance explícito.

## Pendiente para producción

- [ ] Refresh tokens y revocación.
- [ ] Rate limiting.
- [ ] RLS / cifrado por tenant.
- [ ] Pruebas de concurrencia, contrato y carga.
- [ ] CI/CD y Maven Wrapper.
- [ ] Observabilidad completa.
- [ ] Worker de webhooks separado.
- [ ] Conciliación contable.
- [ ] Integración con `mobile/`.
