import { createContext, useContext, useState } from 'react';
import { leerClaims, rolesDelToken, tieneAlgunRol, tokenVigente } from '../utils/jwt.js';

const AuthContext = createContext(null);

// El JWT vive solo en memoria: recargar la pagina cierra la sesion.
export function AuthProvider({ children }) {
  const [token, setToken] = useState('');
  const claims = leerClaims(token);
  const authenticated = tokenVigente(claims);
  const roles = authenticated ? rolesDelToken(claims) : [];

  function iniciarSesion(data) {
    const parsed = leerClaims(data.token);
    if (!tokenVigente(parsed)) throw new Error('El token recibido es inválido o está vencido.');
    setToken(data.token);
  }

  function cerrarSesion() { setToken(''); }

  return <AuthContext.Provider value={{
    token: authenticated ? token : '',
    authenticated,
    roles,
    username: authenticated ? claims.sub : '',
    tieneRol: (...pedidos) => tieneAlgunRol(roles, pedidos),
    iniciarSesion,
    cerrarSesion
  }}>
    {children}
  </AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth requiere AuthProvider');
  return ctx;
}
