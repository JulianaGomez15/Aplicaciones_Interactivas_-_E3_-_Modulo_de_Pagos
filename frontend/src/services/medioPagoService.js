import { http } from './http.js';

// API-07 (CU-08): el usuario dueño del medio sale del JWT, no del cuerpo.
export async function registrarMedioPago(token, tipo, numero) {
  return (await http('/api/v1/payment-methods', { method: 'POST', body: { tipo, numero }, token })).data;
}
