import { useCallback, useEffect, useState } from 'react';
import { listarPagos } from '../services/pagoService.js';
import { useAuth } from '../context/AuthContext.jsx';
import { corto, ESTADOS_PAGO, formatoFecha, formatoMonto, PEDIDOS, USUARIOS } from '../data/demo.js';
import EstadoBadge from '../components/EstadoBadge.jsx';
import PagoDetalle from '../components/PagoDetalle.jsx';

const SIN_FILTROS = { estado: '', codigoUsuario: '', codigoPedido: '', desde: '', hasta: '' };

// CU-06 / CU-07: listado paginado y filtrable. El filtro por pedido devuelve los intentos de pago de ese pedido.
export default function PagosPage() {
  const { token, tieneRol } = useAuth();
  const [filtros, setFiltros] = useState(SIN_FILTROS);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState('fechaCreacion,desc');
  const [page, setPage] = useState(0);
  const [pagina, setPagina] = useState(null);
  const [seleccionado, setSeleccionado] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  // El analista puede listar pero la API no le permite consultar un pago puntual (API-04).
  const puedeVerDetalle = tieneRol('ADMINISTRADOR', 'SERVICIO_LECTURA');

  const cargar = useCallback(async () => {
    setLoading(true);
    try { setPagina(await listarPagos(token, { ...filtros, page, size, sort })); setError(''); }
    catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }, [token, filtros, page, size, sort]);

  useEffect(() => { cargar(); }, [cargar]);

  function cambiar(campo, valor) { setPage(0); setFiltros(f => ({ ...f, [campo]: valor })); }

  return <>
    <section className="panel"><p className="eyebrow">Área privada · ADMINISTRADOR, ANALISTA, SERVICIO_LECTURA</p><h1>Pagos</h1>
      <div className="form grid2">
        <label>Estado<select value={filtros.estado} onChange={e => cambiar('estado', e.target.value)}>
          <option value="">Todos</option>{ESTADOS_PAGO.map(s => <option key={s}>{s}</option>)}</select></label>
        <label>Usuario<select value={filtros.codigoUsuario} onChange={e => cambiar('codigoUsuario', e.target.value)}>
          <option value="">Todos</option>{USUARIOS.map(u => <option key={u.codigo} value={u.codigo}>{u.nombre}</option>)}</select></label>
        <label>Pedido<select value={filtros.codigoPedido} onChange={e => cambiar('codigoPedido', e.target.value)}>
          <option value="">Todos</option>{PEDIDOS.map(p => <option key={p.codigo} value={p.codigo}>…{p.codigo.slice(-1)} · {formatoMonto(p.total)}</option>)}</select></label>
        <label>Desde<input type="date" value={filtros.desde} onChange={e => cambiar('desde', e.target.value)} /></label>
        <label>Hasta<input type="date" value={filtros.hasta} onChange={e => cambiar('hasta', e.target.value)} /></label>
        <label>Orden<select value={sort} onChange={e => setSort(e.target.value)}>
          <option value="fechaCreacion,desc">Más recientes</option><option value="fechaCreacion,asc">Más antiguos</option>
          <option value="monto,desc">Mayor monto</option><option value="monto,asc">Menor monto</option></select></label>
        <label>Por página<select value={size} onChange={e => { setPage(0); setSize(Number(e.target.value)); }}>
          {[5, 10, 20, 50].map(n => <option key={n}>{n}</option>)}</select></label>
      </div>
      <button className="secondary" onClick={cargar}>Recargar</button>
      {loading && <p role="status">Cargando pagos…</p>}
      {error && <p role="alert" className="error">{error}</p>}
      {pagina && !error && <>
        <table><thead><tr><th>Código</th><th>Pedido</th><th>Medio de pago</th><th>Monto</th><th>Estado</th><th>Creado</th>{puedeVerDetalle && <th/>}</tr></thead>
          <tbody>{pagina.content.map(p => <tr key={p.codigoPago} className={p.codigoPago === seleccionado ? 'selected' : ''}>
            <td title={p.codigoPago}>{corto(p.codigoPago)}</td><td title={p.codigoPedido}>{corto(p.codigoPedido)}</td>
            <td>{p.medioPago.tipo} · {p.medioPago.detalleEnmascarado}</td><td>{formatoMonto(p.monto)}</td>
            <td><EstadoBadge estado={p.estado}/></td><td>{formatoFecha(p.fechaCreacion)}</td>
            {puedeVerDetalle && <td><button className="secondary" onClick={() => setSeleccionado(p.codigoPago)}>Ver</button></td>}</tr>)}
            {pagina.content.length === 0 && <tr><td colSpan="7">Sin resultados.</td></tr>}</tbody></table>
        <div className="pager">
          <button className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>‹ Anterior</button>
          <span>Página {pagina.page + 1} de {Math.max(pagina.totalPages, 1)} · {pagina.totalElements} pagos</span>
          <button className="secondary" disabled={page + 1 >= pagina.totalPages} onClick={() => setPage(page + 1)}>Siguiente ›</button>
        </div></>}
    </section>
    {seleccionado && <PagoDetalle key={seleccionado} codigoPago={seleccionado} onChanged={cargar} />}
  </>;
}
