import {useState} from 'react';
import {listarPagos} from '../services/pagosService.js';
import {corto, formatoMonto, PEDIDOS, tieneRol} from '../data/demo.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import PagoDetalle, {EstadoBadge} from '../components/PagoDetalle.jsx';

// CU-04 (por codigo de pago; un cliente solo ve los suyos) y CU-07 (por pedido, requiere permiso de listado).
export default function ConsultaPage({session}) {
    const puedePorPedido = tieneRol(session, 'ADMINISTRADOR', 'ANALISTA', 'SERVICIO_LECTURA');

    const [codigoPago, setCodigoPago] = useState('');
    const [consultado, setConsultado] = useState(null);

    const [codigoPedido, setCodigoPedido] = useState(PEDIDOS[0].codigo);
    const [pagosPedido, setPagosPedido] = useState(null);
    const [error, setError] = useState(null);

    async function buscarPorPedido(e) {
        e.preventDefault();
        try {
            const pagina = await listarPagos(session.token, {codigoPedido, size: 50, sort: 'fechaCreacion,desc'});
            setPagosPedido(pagina.content);
            setError(null);
        } catch (err) {
            setPagosPedido(null);
            setError(err);
        }
    }

    return (
        <>
            <section className="card">
                <h2>Consultar pago</h2>
                <form className="inline" onSubmit={(e) => {
                    e.preventDefault();
                    setConsultado(codigoPago.trim());
                }}>
                    <input className="grow" placeholder="Código de pago (UUID)" value={codigoPago}
                           onChange={(e) => setCodigoPago(e.target.value)}/>
                    <button className="primary">Buscar</button>
                </form>

                {puedePorPedido && (
                    <>
                        <h3>Intentos de pago de un pedido</h3>
                        <form className="inline" onSubmit={buscarPorPedido}>
                            <select className="grow" value={codigoPedido} onChange={(e) => setCodigoPedido(e.target.value)}>
                                {PEDIDOS.map((p) => (
                                    <option key={p.codigo} value={p.codigo}>
                                        …{p.codigo.slice(-1)} · {formatoMonto(p.total)} · {p.estado}
                                    </option>
                                ))}
                            </select>
                            <button className="primary">Buscar</button>
                        </form>
                        <ErrorBanner error={error}/>
                        {pagosPedido && (
                            <table>
                                <thead><tr><th>Código</th><th>Monto</th><th>Estado</th><th/></tr></thead>
                                <tbody>
                                {pagosPedido.map((p) => (
                                    <tr key={p.codigoPago}>
                                        <td title={p.codigoPago}>{corto(p.codigoPago)}…</td>
                                        <td>{formatoMonto(p.monto)}</td>
                                        <td><EstadoBadge estado={p.estado}/></td>
                                        <td><button className="chip" onClick={() => setConsultado(p.codigoPago)}>Ver</button></td>
                                    </tr>
                                ))}
                                {pagosPedido.length === 0 && <tr><td colSpan="4" className="muted">El pedido no tiene pagos</td></tr>}
                                </tbody>
                            </table>
                        )}
                    </>
                )}
            </section>
            {consultado && <PagoDetalle key={consultado} session={session} codigoPago={consultado}/>}
        </>
    );
}
