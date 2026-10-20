import { http, queryString } from './http.js';

const PAGOS = '/api/v1/payments';

// API-01 (CU-01 / CU-09). Devuelve {status, data}: 201 = pago nuevo, 200 = ya existía (idempotencia).
export const iniciarPago = (token, solicitud) => http(PAGOS, { method: 'POST', body: solicitud, token });

// API-02 / API-03
export const aprobarPago = async (token, codigoPago) =>
  (await http(`${PAGOS}/${codigoPago}/approve`, { method: 'POST', token })).data;

export const rechazarPago = async (token, codigoPago, motivo) =>
  (await http(`${PAGOS}/${codigoPago}/reject`, { method: 'POST', body: { motivo: motivo || null }, token })).data;

// API-05. Devuelve {status, data}: 201 = reembolso nuevo, 200 = la clave ya se usó.
export const reembolsarPago = (token, codigoPago, monto, motivo, claveControl) =>
  http(`${PAGOS}/${codigoPago}/refund`, { method: 'POST', body: { monto, motivo, claveControl }, token });

// API-04
export const consultarPago = async (token, codigoPago) => (await http(`${PAGOS}/${codigoPago}`, { token })).data;

// API-06 (CU-06 y, con codigoPedido, CU-07)
export const listarPagos = async (token, filtros) => (await http(`${PAGOS}${queryString(filtros)}`, { token })).data;

// API-08
export const historialPago = async (token, codigoPago) => (await http(`${PAGOS}/${codigoPago}/history`, { token })).data;
