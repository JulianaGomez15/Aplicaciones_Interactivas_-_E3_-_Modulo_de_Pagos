import {queryString, rawRequest, request} from './api.js';

const PAGOS = '/api/v1/payments';

export function login(username, password) {
    return request('/auth/login', {method: 'POST', body: {username, password}});
}

// API-01 (CU-01 / CU-09). Devuelve {status, data}: 201 = pago nuevo, 200 = ya existia (idempotencia).
export function iniciarPago(token, solicitud) {
    return rawRequest(PAGOS, {method: 'POST', body: solicitud, token});
}

export const aprobarPago = (token, codigoPago) =>
    request(`${PAGOS}/${codigoPago}/approve`, {method: 'POST', token});

export const rechazarPago = (token, codigoPago, motivo) =>
    request(`${PAGOS}/${codigoPago}/reject`, {method: 'POST', body: {motivo: motivo || null}, token});

// API-05. Devuelve {status, data}: 201 = reembolso nuevo, 200 = la clave ya se uso.
export const reembolsarPago = (token, codigoPago, monto, motivo, claveControl) =>
    rawRequest(`${PAGOS}/${codigoPago}/refund`, {method: 'POST', body: {monto, motivo, claveControl}, token});

export const obtenerPago = (token, codigoPago) => request(`${PAGOS}/${codigoPago}`, {token});

// API-06 (CU-06 y, con codigoPedido, CU-07).
export const listarPagos = (token, filtros) => request(`${PAGOS}${queryString(filtros)}`, {token});

export const historialPago = (token, codigoPago) => request(`${PAGOS}/${codigoPago}/history`, {token});

// API-07: el usuario sale del JWT.
export const registrarMedioPago = (token, tipo, numero) =>
    request('/api/v1/payment-methods', {method: 'POST', body: {tipo, numero}, token});
