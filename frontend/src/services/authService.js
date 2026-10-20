import { ApiError, http } from './http.js';

export async function login(username, password) {
  try {
    const { data } = await http('/auth/login', { method: 'POST', body: { username, password } });
    if (!data?.token) throw new Error('La API no devolvió un JWT.');
    return data;
  } catch (err) {
    if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
      throw new Error('Usuario o contraseña incorrectos.');
    }
    throw err;
  }
}
