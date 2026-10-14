// Datos de demo que simulan a los modulos vecinos (Usuarios y Pedidos).
// Deben coincidir con UsuariosClientSimulado y PedidosClientSimulado del backend.

export const USUARIOS = [
    {codigo: '00000000-0000-0000-0000-000000000001', nombre: 'Ana Perez'},
    {codigo: '00000000-0000-0000-0000-000000000002', nombre: 'Bruno Diaz'},
    {codigo: '00000000-0000-0000-0000-000000000003', nombre: 'Carla Lopez (inactiva)'}
];

export const PEDIDOS = [
    {codigo: '00000000-0000-0000-0001-000000000001', total: 15000.0, estado: 'CONFIRMADO'},
    {codigo: '00000000-0000-0000-0001-000000000002', total: 8500.5, estado: 'CONFIRMADO'},
    {codigo: '00000000-0000-0000-0001-000000000003', total: 32000.0, estado: 'CONFIRMADO'},
    {codigo: '00000000-0000-0000-0001-000000000004', total: 4200.0, estado: 'CANCELADO'}
];

export const CUENTAS = [
    {username: 'admin', password: 'admin123', rol: 'ADMIN'},
    {username: 'pedidos', password: 'pedidos123', rol: 'PEDIDOS'},
    {username: 'analista', password: 'analista123', rol: 'ANALISTA'},
    {username: 'cliente', password: 'cliente123', rol: 'CLIENTE'}
];

export const TIPOS_MEDIO_PAGO = ['TARJETA_CREDITO', 'TARJETA_DEBITO', 'BILLETERA_VIRTUAL', 'TRANSFERENCIA'];

export const ESTADOS_PAGO = ['PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO'];

const ars = new Intl.NumberFormat('es-AR', {style: 'currency', currency: 'ARS'});

export const formatoMonto = (n) => ars.format(Number(n));
export const formatoFecha = (iso) => (iso ? new Date(iso).toLocaleString('es-AR') : '—');
export const corto = (uuid) => (uuid ? uuid.slice(0, 8) : '—');
export const nombreUsuario = (codigo) => USUARIOS.find((u) => u.codigo === codigo)?.nombre ?? corto(codigo);
