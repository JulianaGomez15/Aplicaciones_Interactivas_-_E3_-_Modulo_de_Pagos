import { tieneAlgunRol } from './jwt.js';

// Roles autorizados por pantalla, segun la tabla de autorizacion del TPO.
// El backend vuelve a validarlos en cada request; esto solo ordena la navegacion.
export const RUTAS_PRIVADAS = [
  { path: '/servicio/pagos/nuevo', titulo: 'Iniciar pago', roles: ['SERVICIO_ESCRITURA'] },
  { path: '/admin/pagos', titulo: 'Pagos', roles: ['ADMINISTRADOR', 'ANALISTA', 'SERVICIO_LECTURA'] },
  { path: '/pagos/consulta', titulo: 'Consultar pago', roles: ['CLIENTE', 'ADMINISTRADOR', 'SERVICIO_LECTURA'] },
  { path: '/cliente/medios-pago', titulo: 'Medios de pago', roles: ['CLIENTE'] }
];

export function rutasPermitidas(roles) {
  return RUTAS_PRIVADAS.filter(r => tieneAlgunRol(roles, r.roles));
}

// Pantalla a la que se va despues de iniciar sesion.
export function rutaInicial(roles) {
  return rutasPermitidas(roles)[0]?.path ?? '/inicio';
}
