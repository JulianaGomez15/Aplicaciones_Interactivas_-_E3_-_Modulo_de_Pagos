import {useCallback, useEffect, useState} from 'react';
import {listarPagos} from '../services/pagosService.js';
import {corto, ESTADOS_PAGO, formatoFecha, formatoMonto, nombreUsuario, USUARIOS} from '../data/demo.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import PagoDetalle, {EstadoBadge} from '../components/PagoDetalle.jsx';

// CU-06: listado paginado y filtrable.
export default function PagosPage({session}) {
    const [filtros, setFiltros] = useState({estado: '', codigoUsuario: '', desde: '', hasta: ''});
    const [size, setSize] = useState(10);
    const [sort, setSort] = useState('fechaCreacion,desc');
    const [page, setPage] = useState(0);
    const [pagina, setPagina] = useState(null);
    const [seleccionado, setSeleccionado] = useState(null);
    const [error, setError] = useState(null);

    const cargar = useCallback(async () => {
        try {
            setPagina(await listarPagos(session.token, {...filtros, page, size, sort}));
            setError(null);
        } catch (e) {
            setError(e);
        }
    }, [session.token, filtros, page, size, sort]);

    useEffect(() => {
        cargar();
    }, [cargar]);

    function cambiarFiltro(campo, valor) {
        setPage(0);
        setFiltros((f) => ({...f, [campo]: valor}));
    }

    return (
        <>
            <section className="card">
                <h2>Pagos</h2>
                <div className="form-grid">
                    <label>Estado
                        <select value={filtros.estado} onChange={(e) => cambiarFiltro('estado', e.target.value)}>
                            <option value="">Todos</option>
                            {ESTADOS_PAGO.map((s) => <option key={s}>{s}</option>)}
                        </select>
                    </label>
                    <label>Usuario
                        <select value={filtros.codigoUsuario}
                                onChange={(e) => cambiarFiltro('codigoUsuario', e.target.value)}>
                            <option value="">Todos</option>
                            {USUARIOS.map((u) => <option key={u.codigo} value={u.codigo}>{u.nombre}</option>)}
                        </select>
                    </label>
                    <label>Desde
                        <input type="date" value={filtros.desde} onChange={(e) => cambiarFiltro('desde', e.target.value)}/>
                    </label>
                    <label>Hasta
                        <input type="date" value={filtros.hasta} onChange={(e) => cambiarFiltro('hasta', e.target.value)}/>
                    </label>
                    <label>Orden
                        <select value={sort} onChange={(e) => setSort(e.target.value)}>
                            <option value="fechaCreacion,desc">Más recientes</option>
                            <option value="fechaCreacion,asc">Más antiguos</option>
                            <option value="monto,desc">Mayor monto</option>
                            <option value="monto,asc">Menor monto</option>
                        </select>
                    </label>
                    <label>Por página
                        <select value={size} onChange={(e) => {
                            setPage(0);
                            setSize(Number(e.target.value));
                        }}>
                            {[5, 10, 20, 50].map((n) => <option key={n}>{n}</option>)}
                        </select>
                    </label>
                </div>
                <ErrorBanner error={error}/>

                {pagina && (
                    <>
                        <table>
                            <thead>
                            <tr><th>Código</th><th>Pedido</th><th>Usuario</th><th>Monto</th><th>Estado</th>
                                <th>Creado</th><th/></tr>
                            </thead>
                            <tbody>
                            {pagina.content.map((p) => (
                                <tr key={p.codigo} className={p.codigo === seleccionado ? 'selected' : ''}>
                                    <td title={p.codigo}>{corto(p.codigo)}…</td>
                                    <td title={p.codigoPedido}>{corto(p.codigoPedido)}…</td>
                                    <td>{nombreUsuario(p.codigoUsuario)}</td>
                                    <td>{formatoMonto(p.monto)}</td>
                                    <td><EstadoBadge estado={p.estado}/></td>
                                    <td>{formatoFecha(p.fechaCreacion)}</td>
                                    <td><button className="chip" onClick={() => setSeleccionado(p.codigo)}>Ver</button></td>
                                </tr>
                            ))}
                            {pagina.content.length === 0 && <tr><td colSpan="7" className="muted">Sin resultados</td></tr>}
                            </tbody>
                        </table>
                        <div className="pager">
                            <button className="chip" disabled={page === 0} onClick={() => setPage(page - 1)}>‹ Anterior</button>
                            <span>Página {pagina.page + 1} de {Math.max(pagina.totalPages, 1)} · {pagina.totalElements} pagos</span>
                            <button className="chip" disabled={page + 1 >= pagina.totalPages}
                                    onClick={() => setPage(page + 1)}>Siguiente ›</button>
                            <button className="chip" onClick={cargar}>Recargar</button>
                        </div>
                    </>
                )}
            </section>
            {seleccionado && (
                <PagoDetalle key={seleccionado} session={session} codigo={seleccionado} onChanged={cargar}/>
            )}
        </>
    );
}
