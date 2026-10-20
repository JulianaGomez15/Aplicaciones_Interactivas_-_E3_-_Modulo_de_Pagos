export const BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8089').replace(/\/$/, '');

// Error con el formato normalizado de la API: {status, code, message, correlationId}.
export class ApiError extends Error {
  constructor(status, code, message, correlationId) {
    super(status ? `${code} (HTTP ${status}): ${message}` : message);
    this.status = status;
    this.code = code;
    this.correlationId = correlationId;
  }
}

// Devuelve {status, data}. Lanza ApiError ante cualquier respuesta no exitosa.
export async function http(path, { method = 'GET', body, token } = {}) {
  const correlationId = crypto.randomUUID();
  const headers = { 'X-Correlation-Id': correlationId };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method, headers, body: body === undefined ? undefined : JSON.stringify(body)
    });
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'No se pudo conectar con el backend. Verificá Spring Boot y CORS.', correlationId);
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, data?.code ?? `HTTP_${response.status}`,
      data?.message ?? 'Error al consultar la API.', data?.correlationId ?? correlationId);
  }
  return { status: response.status, data };
}

export function queryString(params) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') q.set(k, v); });
  const s = q.toString();
  return s ? `?${s}` : '';
}
