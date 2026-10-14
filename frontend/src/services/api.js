export const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8089';

// Error con el formato de la API: {status, code, message, correlationId}.
export class ApiError extends Error {
    constructor(status, code, message, correlationId) {
        super(message);
        this.status = status;
        this.code = code;
        this.correlationId = correlationId;
    }
}

// Devuelve {status, data}. Lanza ApiError ante cualquier respuesta no exitosa.
export async function rawRequest(path, {method = 'GET', body, token} = {}) {
    const correlationId = crypto.randomUUID();
    const headers = {'X-Correlation-Id': correlationId};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (token) headers.Authorization = `Bearer ${token}`;

    let r;
    try {
        r = await fetch(`${BASE_URL}${path}`, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body)
        });
    } catch {
        throw new ApiError(0, 'NETWORK_ERROR', 'No se pudo conectar con el backend', correlationId);
    }

    const data = await r.json().catch(() => null);
    if (!r.ok) {
        throw new ApiError(
            r.status,
            data?.code ?? `HTTP_${r.status}`,
            data?.message ?? `Error HTTP ${r.status}`,
            data?.correlationId ?? correlationId
        );
    }
    return {status: r.status, data};
}

export async function request(path, options) {
    return (await rawRequest(path, options)).data;
}

export function queryString(params) {
    const q = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') q.set(k, v);
    });
    const s = q.toString();
    return s ? `?${s}` : '';
}
