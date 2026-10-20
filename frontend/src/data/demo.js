// Datos de demo que simulan a los modulos vecinos (Usuarios y Pedidos).
// Deben coincidir con UsuariosClientSimulado, PedidosClientSimulado y CuentasDemo del backend.

export const USUARIOS = [
  { codigo: '00000000-0000-0000-0000-000000000001', nombre: 'Ana Perez' },
  { codigo: '00000000-0000-0000-0000-000000000002', nombre: 'Bruno Diaz' },
  { codigo: '00000000-0000-0000-0000-000000000003', nombre: 'Carla Lopez (inactiva)' }
];

export const PEDIDOS = [
  { codigo: '00000000-0000-0000-0001-000000000001', total: 15000.0, estado: 'BORRADOR' },
  { codigo: '00000000-0000-0000-0001-000000000002', total: 8500.5, estado: 'BORRADOR' },
  { codigo: '00000000-0000-0000-0001-000000000003', total: 32000.0, estado: 'BORRADOR' },
  { codigo: '00000000-0000-0000-0001-000000000004', total: 4200.0, estado: 'CANCELADO' }
];

export const CUENTAS = [
  { username: 'api-pedidos', password: 'pedidos123', descripcion: 'Servicio de Pedidos (escribe y lee)' },
  { username: 'cliente', password: 'cliente123', descripcion: 'Cliente Ana' },
  { username: 'cliente2', password: 'cliente123', descripcion: 'Cliente Bruno' },
  { username: 'admin', password: 'admin123', descripcion: 'Administrador' },
  { username: 'analista', password: 'analista123', descripcion: 'Analista' },
  { username: 'api-analitica', password: 'analitica123', descripcion: 'Servicio de Analítica (lectura)' }
];

export const TIPOS_MEDIO_PAGO = ['TARJETA_CREDITO', 'TARJETA_DEBITO', 'BILLETERA_VIRTUAL', 'TRANSFERENCIA'];
export const ESTADOS_PAGO = ['PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO'];

const ars = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
export const formatoMonto = n => ars.format(Number(n));
export const formatoFecha = iso => (iso ? new Date(iso).toLocaleString('es-AR') : '—');
export const corto = uuid => (uuid ? `${uuid.slice(0, 8)}…` : '—');
