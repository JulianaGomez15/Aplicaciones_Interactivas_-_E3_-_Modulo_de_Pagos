import {queryString, rawRequest, request} from './api.js';

const PAGOS = '/api/v1/payments';
const MEDIOS = '/api/v1/payment-methods';

export function login(username, password) {
    return request('/auth/login', {method: 'POST', body: {username, password}});
}

// CU-01 / CU-09. Devuelve {status, data}: 201 = pago nuevo, 200 = ya existia (idempotencia).
export function iniciarPago(token, solicitud) {
    return rawRequest(PAGOS, {method: 'POST', body: solicitud, token});
}

export const aprobarPago = (token, codigo) =>
    request(`${PAGOS}/${codigo}/approve`, {method: 'POST', token});

export const rechazarPago = (token, codigo, motivo) =>
    request(`${PAGOS}/${codigo}/reject`, {method: 'POST', body: {motivo: motivo || null}, token});

export const reembolsarPago = (token, codigo, monto, motivo) =>
    request(`${PAGOS}/${codigo}/refund`, {method: 'POST', body: {monto, motivo}, token});

export const obtenerPago = (token, codigo) => request(`${PAGOS}/${codigo}`, {token});

export const listarPagos = (token, filtros) => request(`${PAGOS}${queryString(filtros)}`, {token});

export const pagosPorPedido = (token, codigoPedido) => request(`${PAGOS}/order/${codigoPedido}`, {token});

export const historialPago = (token, codigo) => request(`${PAGOS}/${codigo}/history`, {token});

export const reembolsosPago = (token, codigo) => request(`${PAGOS}/${codigo}/refunds`, {token});

export const crearMedioPago = (token, codigoUsuario, tipo, detalle) =>
    request(MEDIOS, {method: 'POST', body: {codigoUsuario, tipo, detalle}, token});

export const listarMediosPago = (token, codigoUsuario) =>
    request(`${MEDIOS}${queryString({codigoUsuario})}`, {token});
