import {useState} from 'react';
import {pagosPorPedido} from '../services/pagosService.js';
import {corto, formatoMonto, PEDIDOS} from '../data/demo.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import PagoDetalle, {EstadoBadge} from '../components/PagoDetalle.jsx';

// CU-04 (por codigo de pago) y CU-07 (por pedido, solo ADMIN / ANALISTA).
export default function ConsultaPage({session}) {
    const puedePorPedido = session.rol === 'ADMIN' || session.rol === 'ANALISTA';

    const [codigoPago, setCodigoPago] = useState('');
    const [consultado, setConsultado] = useState(null);

    const [codigoPedido, setCodigoPedido] = useState(PEDIDOS[0].codigo);
    const [pagosPedido, setPagosPedido] = useState(null);
    const [error, setError] = useState(null);

    async function buscarPorPedido(e) {
        e.preventDefault();
        try {
            setPagosPedido(await pagosPorPedido(session.token, codigoPedido));
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
                        <h3>Pagos de un pedido</h3>
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
                                    <tr key={p.codigo}>
                                        <td title={p.codigo}>{corto(p.codigo)}…</td>
                                        <td>{formatoMonto(p.monto)}</td>
                                        <td><EstadoBadge estado={p.estado}/></td>
                                        <td><button className="chip" onClick={() => setConsultado(p.codigo)}>Ver</button></td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        )}
                    </>
                )}
            </section>
            {consultado && <PagoDetalle key={consultado} session={session} codigo={consultado}/>}
        </>
    );
}
