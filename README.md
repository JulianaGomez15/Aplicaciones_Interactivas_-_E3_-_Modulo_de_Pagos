# DeliveryGo · Módulo de Pagos (Grupo 3)

Backend Spring Boot 3.5 (Java 25, JPA + H2, JWT, Swagger) y frontend React/Vite.
Los pagos son **simulados**: no se procesa dinero real ni se guardan datos reales de tarjetas.

## Ejecutar

```bash
# Backend (puerto 8089)
mvn spring-boot:run          # o correr PagosApplication desde IntelliJ

# Frontend (puerto 5173, Node 24)
cd frontend
cp .env.example .env
npm install
npm run dev
```

- Swagger UI: http://localhost:8089/swagger-ui.html
- Consola H2: http://localhost:8089/h2-console (`jdbc:h2:mem:pagos`, usuario `sa`, sin contraseña)
- Tests: `mvn test`

## Usuarios de demo

| Usuario  | Contraseña    | Rol       | Puede                                                         |
|----------|---------------|-----------|---------------------------------------------------------------|
| admin    | admin123      | ADMIN     | todo                                                          |
| pedidos  | pedidos123    | PEDIDOS   | iniciar, aprobar, rechazar, reembolsar, consultar             |
| analista | analista123   | ANALISTA  | listar, historial, pagos por pedido, reembolsos (solo lectura)|
| cliente  | cliente123    | CLIENTE   | medios de pago, consultar un pago                             |

`POST /auth/login` → `{token}` → header `Authorization: Bearer <token>`.

## Datos simulados de los módulos vecinos

Pedidos, Usuarios y Notificaciones están simulados en `integration/*ClientSimulado`.

| Pedido                                  | Total       | Estado     |
|-----------------------------------------|-------------|------------|
| `00000000-0000-0000-0001-000000000001`  | 15000.00    | CONFIRMADO (demo: pago aprobado y reembolsado parcialmente) |
| `00000000-0000-0000-0001-000000000002`  | 8500.50     | CONFIRMADO (demo: pago rechazado, se puede reintentar con clave nueva) |
| `00000000-0000-0000-0001-000000000003`  | 32000.00    | CONFIRMADO (libre) |
| `00000000-0000-0000-0001-000000000004`  | 4200.00     | CANCELADO  |

Usuarios: `…0000-000000000001` Ana, `…000000000002` Bruno, `…000000000003` Carla (inactiva).

## API

| Caso de uso | Endpoint | Roles |
|---|---|---|
| CU-01 / CU-09 Iniciar pago (idempotente) | `POST /api/v1/payments` | ADMIN, PEDIDOS |
| CU-02 Autorizar | `POST /api/v1/payments/{codigoPago}/approve` | ADMIN, PEDIDOS |
| CU-03 Rechazar | `POST /api/v1/payments/{codigoPago}/reject` | ADMIN, PEDIDOS |
| CU-04 Consultar | `GET /api/v1/payments/{codigoPago}` | autenticado |
| CU-05 Reembolsar | `POST /api/v1/payments/{codigoPago}/refund` | ADMIN, PEDIDOS |
| CU-06 Listar (filtros `estado`, `codigoUsuario`, `desde`, `hasta`; `page`, `size`, `sort`) | `GET /api/v1/payments` | ADMIN, ANALISTA |
| CU-07 Pagos de un pedido | `GET /api/v1/payments/order/{codigoPedido}` | ADMIN, ANALISTA |
| CU-08 Medios de pago | `POST` / `GET /api/v1/payment-methods?codigoUsuario=` | ADMIN, CLIENTE |
| CU-10 Historial | `GET /api/v1/payments/{codigoPago}/history` | ADMIN, ANALISTA |
| Reembolsos | `GET /api/v1/payments/{codigoPago}/refunds`, `GET /api/v1/refunds/{codigo}` | ADMIN, ANALISTA, PEDIDOS |

Errores: `{timestamp, status, code, message, path, correlationId}` (el `correlationId` sale del header
`X-Correlation-Id`, o se genera). Códigos: `PAYMENT_NOT_FOUND`, `PAYMENT_AMOUNT_MISMATCH`,
`PAYMENT_INVALID_STATE`, `DUPLICATE_PAYMENT_REQUEST`, `REFUND_EXCEEDS_AMOUNT`, `PAYMENT_METHOD_INVALID`,
`REFUND_NOT_FOUND` (+ `USER_INVALID`, `ORDER_NOT_PAYABLE`, `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`).

## Decisiones donde el enunciado era ambiguo

- **CU-07**: el enunciado usa `GET /api/v1/payments/{idPedido}`, que choca con `GET /{codigoPago}`; se expone como `/order/{codigoPedido}`.
- **RN-10 (idempotencia)**: aprobar un pago ya APROBADO y rechazar uno ya RECHAZADO devuelven 200 sin cambios; cualquier otra transición inválida es 409 `PAYMENT_INVALID_STATE`
  (el texto de CU-03 sobre "rechazar un pago APROBADO → 200" contradice a RN-04, se priorizó RN-04).
- **RN-05**: repetir la clave de un pago RECHAZADO devuelve ese pago (no crea otro); un reintento real exige clave nueva.
  Un pedido no admite un segundo pago mientras tenga uno PENDIENTE o APROBADO.
- **Reembolso total**: cuando la suma de reembolsos alcanza el monto aprobado, el pago pasa a REEMBOLSADO; mientras sea parcial queda APROBADO.
- Se usa una única excepción de negocio (`PagoException` + `ErrorCode`) porque el TPO exige código y HTTP status propios por error.
