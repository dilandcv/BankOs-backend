# Visión general de la API

## Estado
- **Implementado:** ninguno. No existen controladores, rutas, configuración web, ni especificación OpenAPI/Swagger en el backend.
- **Propuesto:** API REST JSON, versionada bajo `/api/v1`, segura con JWT y diseñada para consumo independiente (herramientas HTTP/pruebas), sin integración con el frontend en esta fase.

## Principios (propuestos)

- **Estilo:** REST sobre HTTP, intercambio en JSON (`Content-Type: application/json`).
- **Versionado:** prefijo `/api/v1` (propuesto).
- **Seguridad:** autenticación mediante `Authorization: Bearer <JWT>`; autorización por roles y aislamiento por tenant desde el contexto autenticado.
- **Trazabilidad:** `X-Correlation-ID` (UUID) en solicitudes/respuestas y registros.
- **Idempotencia:** operaciones financieras aceptan `Idempotency-Key`.
- **Consistencia:** formato estándar de respuestas exitosas y de error; validación de entrada con Jakarta Validation.

## Encabezados

### Generales (propuestos)
```http
Authorization: Bearer <JWT>
Content-Type: application/json
X-Correlation-ID: <UUID>
```

### Operaciones financieras (propuestos)
```http
Idempotency-Key: <UNIQUE_KEY>
```

- `Authorization`: requerido en endpoints protegidos (obtenido vía login, cuando se implemente).
- `X-Correlation-ID`: recomendado/obligatorio según política a definir; si no se recibe, el servidor puede generarlo (propuesta).
- `Idempotency-Key`: requerido en creación/ejecución de operaciones financieras para evitar duplicados.

## Formato de respuesta (propuesto)

### Éxito
```json
{
  "success": true,
  "code": "OPERATION_SUCCESS",
  "message": "Operación realizada correctamente",
  "data": {},
  "correlation_id": "uuid"
}
```

### Error
```json
{
  "success": false,
  "code": "INSUFFICIENT_FUNDS",
  "message": "Saldo insuficiente",
  "data": null,
  "correlation_id": "uuid"
}
```

## Códigos HTTP (orientativos, propuestos)

| HTTP | Uso típico |
|---|---|
| 200 | Operación exitosa / consulta |
| 201 | Recurso creado |
| 400 | Solicitud inválida / validación |
| 401 | No autenticado o token inválido/expirado |
| 403 | Sin permisos o acceso fuera del tenant |
| 404 | Recurso no encontrado (en alcance del tenant) |
| 409 | Conflicto (duplicado, estado inválido, idempotencia en conflicto) |
| 422 | Regla de negocio no satisfecha (si se adopta) |
| 429 | Límite de solicitudes (si se adopta) |
| 500 | Error interno |

> El mapeo exacto de códigos de negocio a HTTP debe confirmarse e implementarse de forma consistente.

## Paginación y filtrado (propuesto)
- Para listados (`GET /accounts`, `GET /transactions`, etc.): parámetros como `page`, `size`/`limit`, `sort` y filtros por fechas/estado según recurso.
- Respuesta paginada sugerida en `data`: elementos, página, tamaño, total de elementos/páginas (formato final pendiente).
- **Estado:** no implementado.

## Endpoints iniciales (propuestos, no implementados)

| Método | Endpoint | Funcionalidad |
|---|---|---|
| POST | `/tenants` | Registrar institución |
| POST | `/auth/login` | Iniciar sesión |
| GET | `/auth/me` | Usuario autenticado |
| GET | `/accounts` | Consultar cuentas |
| GET | `/accounts/{id}` | Consultar cuenta |
| POST | `/transactions/deposits` | Depósito |
| POST | `/transactions/withdrawals` | Retiro |
| POST | `/transactions/transfers/preview` | Previsualizar transferencia |
| POST | `/transactions/transfers` | Ejecutar transferencia |
| GET | `/transactions` | Consultar movimientos |
| GET | `/transactions/{id}` | Consultar transacción |

Endpoints administrativos (usuarios, clientes, cuentas, configuraciones, tasas de cambio, auditoría, webhooks) también deben documentarse y versionarse; ver [Contratos de API](api-contracts.md).

## Consideraciones de independencia
- Los contratos aquí definidos son para desarrollo/pruebas del backend (curl, clientes HTTP de prueba, suites automatizadas).
- **No** se deben crear ni modificar clientes, configuraciones ni código en `mobile/` a partir de estos contratos durante esta fase.