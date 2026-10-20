// Decodificar el payload NO valida firma ni permisos: solo sirve para adaptar la interfaz.
// El control de acceso real lo hace Spring Security en el backend.
export function leerClaims(token) {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const binary = atob(normalized);
    const bytes = Uint8Array.from(binary, ch => ch.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch { return null; }
}

export function tokenVigente(claims) {
  return !!claims && typeof claims.exp === 'number' && claims.exp * 1000 > Date.now();
}

export function rolesDelToken(claims) {
  if (!claims) return [];
  const roles = Array.isArray(claims.roles) ? claims.roles : [claims.role].filter(Boolean);
  return roles.map(r => String(r).replace(/^ROLE_/, ''));
}

// true si el usuario tiene al menos uno de los roles pedidos (o si no se pide ninguno).
export function tieneAlgunRol(rolesUsuario, rolesPedidos = []) {
  return rolesPedidos.length === 0 || rolesPedidos.some(r => rolesUsuario.includes(r));
}
