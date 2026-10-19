# DeliveryGo · Módulo de Pagos (Grupo 3)

Backend Spring Boot 3.5 (Java 25, JPA, JWT, Swagger) y frontend React/Vite.
Los pagos son **simulados**: no se procesa dinero real ni se guardan datos reales de tarjetas.
El contrato sigue el documento de la Entrega 1 corregido (`src/main/java/ar/edu/uade/api/pagos/PDF/`).

## Ejecutar

```bash
# Backend (puerto 8089, H2 en memoria con datos de demo)
mvn spring-boot:run          # o correr PagosApplication desde IntelliJ

# Con PostgreSQL (BL-08): levanta la base y usa el perfil "postgres" (sin datos de demo)
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres

# Frontend (puerto 5173, Node 24)
cd frontend
cp .env.example .env
npm install
npm run dev
```

- **Swagger UI:** http://localhost:8089/swagger-ui.html · OpenAPI JSON: http://localhost:8089/v3/api-docs
- Consola H2: http://localhost:8089/h2-console (`jdbc:h2:mem:pagos`, usuario `sa`, sin contraseña)
- Tests: `mvn test` (incluye `OpenApiContractTest`, que compara el Swagger generado con el contrato del TPO)

## Autenticación y roles

En el ecosistema el JWT lo emite el Servicio de Usuarios; Pagos solo valida firma y rol. Acá `POST /auth/login`
lo simula (Swagger → *Authorize* con el token).

| Usuario / contraseña            | Roles                                  |
|---------------------------------|----------------------------------------|
| `api-pedidos` / `pedidos123`    | SERVICIO_ESCRITURA, SERVICIO_LECTURA   |
| `api-analitica` / `analitica123`| SERVICIO_LECTURA                       |
| `admin` / `admin123`            | ADMINISTRADOR                          |
| `analista` / `analista123`      | ANALISTA                               |
| `cliente` / `cliente123`        | CLIENTE (usuario Ana)                  |
| `cliente2` / `cliente123`       | CLIENTE (usuario Bruno)                |

## API (tabla "APIs" del TPO)

| ID | Endpoint | CU | Roles | HTTP |
|---|---|---|---|---|
| API-01 | `POST /api/v1/payments` | CU-01, CU-09 | SERVICIO_ESCRITURA | 201, 200, 400, 401, 403, 409, 422, 503 |
| API-02 | `POST /api/v1/payments/{codigoPago}/approve` | CU-02 | SERVICIO_ESCRITURA | 200, 401, 403, 404, 409 |
| API-03 | `POST /api/v1/payments/{codigoPago}/reject` | CU-03 | SERVICIO_ESCRITURA | 200, 400, 401, 403, 404, 409 |
| API-04 | `GET /api/v1/payments/{codigoPago}` | CU-04 | CLIENTE (solo sus pagos), ADMINISTRADOR, SERVICIO_LECTURA | 200, 401, 403, 404 |
| API-05 | `POST /api/v1/payments/{codigoPago}/refund` | CU-05 | SERVICIO_ESCRITURA, ADMINISTRADOR | 201, 200, 400, 401, 403, 404, 409, 422 |
| API-06 | `GET /api/v1/payments` (filtros `estado`, `codigoUsuario`, `codigoPedido`, `desde`, `hasta`; `page`, `size`, `sort`) | CU-06, CU-07 | ADMINISTRADOR, ANALISTA, SERVICIO_LECTURA | 200, 400, 401, 403 |
| API-07 | `POST /api/v1/payment-methods` | CU-08 | CLIENTE | 201, 400, 401, 403 |
| API-08 | `GET /api/v1/payments/{codigoPago}/history` | CU-10 | ADMINISTRADOR | 200, 401, 403, 404 |

Errores: `{timestamp, status, code, message, path, correlationId}` (el `correlationId` sale del header
`X-Correlation-Id`, o se genera). Códigos: `PAYMENT_NOT_FOUND`, `PAYMENT_AMOUNT_MISMATCH`, `PAYMENT_INVALID_STATE`,
`DUPLICATE_PAYMENT_REQUEST`, `REFUND_EXCEEDS_AMOUNT`, `PAYMENT_METHOD_INVALID`, `PAYMENT_USER_INVALID`,
`ORDER_NOT_PAYABLE`, `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `EXTERNAL_SERVICE_UNAVAILABLE`, `INTERNAL_ERROR`.

## Módulos vecinos simulados (INT-01 a INT-03)

`integration/*ClientSimulado` reemplaza a Pedidos, Usuarios y Notificaciones hasta que estén los otros módulos.

| Pedido                                  | Total     | Estado    |
|-----------------------------------------|-----------|-----------|
| `00000000-0000-0000-0001-000000000001`  | 15000.00  | BORRADOR (demo: pago aprobado y reembolsado parcialmente) |
| `00000000-0000-0000-0001-000000000002`  | 8500.50   | BORRADOR (demo: pago rechazado; se reintenta con otra clave) |
| `00000000-0000-0000-0001-000000000003`  | 32000.00  | BORRADOR (libre) |
| `00000000-0000-0000-0001-000000000004`  | 4200.00   | CANCELADO → `ORDER_NOT_PAYABLE` |
| `00000000-0000-0000-0001-000000000099`  | —         | simula que Pedidos no responde → 503 |

Usuarios: `00000000-0000-0000-0000-000000000001` Ana, `…02` Bruno, `…03` inactiva (`PAYMENT_USER_INVALID`),
`…99` simula que Usuarios no responde (503).

## Decisiones donde el documento era ambiguo o incompleto

- **Medios de pago:** la API solo tiene el alta (API-07), sin listado. El front recuerda en `localStorage` los
  que se registraron en ese navegador (solo datos enmascarados) para poder usarlos en «Iniciar pago».
- **CU-05, reembolsos:** la clave de control repetida con otro monto o sobre otro pago responde 409
  `DUPLICATE_PAYMENT_REQUEST`; con los mismos datos, 200 con el reembolso existente.
- **Concurrencia:** dos `POST /payments` simultáneos con la misma clave → un solo pago, ambos reciben el mismo
  (CU-09 4b, verificado en `PagosApiTest`). Los reembolsos sobre un mismo pago se serializan con bloqueo.
- **Un pedido puede tener varios intentos** (RN-05); no se bloquea un segundo pago mientras haya uno PENDIENTE.
- **Códigos técnicos extra** fuera de la tabla del TPO: `NOT_FOUND` (ruta inexistente) y `METHOD_NOT_ALLOWED`.
- **Una sola excepción de negocio** (`PagoException` + `ErrorCode`): el TPO exige código y HTTP status propios por error.
- El perfil `postgres` usa `ddl-auto=update` y no carga datos de demo.
