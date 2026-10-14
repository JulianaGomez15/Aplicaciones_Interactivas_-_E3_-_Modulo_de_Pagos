import {useCallback, useEffect, useState} from 'react';
import {
    aprobarPago,
    historialPago,
    obtenerPago,
    rechazarPago,
    reembolsarPago,
    reembolsosPago
} from '../services/pagosService.js';
import {corto, formatoFecha, formatoMonto, nombreUsuario} from '../data/demo.js';
import ErrorBanner from './ErrorBanner.jsx';

export function EstadoBadge({estado}) {
    return <span className={`badge ${estado}`}>{estado}</span>;
}

// Detalle de un pago + acciones segun el rol (los permisos reales los valida el backend).
export default function PagoDetalle({session, codigo, onChanged}) {
    const {token, rol} = session;
    const puedeOperar = rol === 'ADMIN' || rol === 'PEDIDOS';
    const puedeAuditar = rol === 'ADMIN' || rol === 'ANALISTA';
    const puedeVerReembolsos = puedeAuditar || rol === 'PEDIDOS';

    const [pago, setPago] = useState(null);
    const [historial, setHistorial] = useState([]);
    const [reembolsos, setReembolsos] = useState([]);
    const [error, setError] = useState(null);
    const [motivoRechazo, setMotivoRechazo] = useState('');
    const [montoReembolso, setMontoReembolso] = useState('');
    const [motivoReembolso, setMotivoReembolso] = useState('');

    const cargar = useCallback(async () => {
        try {
            const p = await obtenerPago(token, codigo);
            setPago(p);
            setHistorial(puedeAuditar ? await historialPago(token, codigo) : []);
            setReembolsos(puedeVerReembolsos ? await reembolsosPago(token, codigo) : []);
            setError(null);
        } catch (e) {
            setPago(null);
            setError(e);
        }
    }, [token, codigo, puedeAuditar, puedeVerReembolsos]);

    useEffect(() => {
        cargar();
    }, [cargar]);

    async function ejecutar(accion) {
        try {
            await accion();
            setError(null);
            await cargar();
            onChanged?.();
        } catch (e) {
            setError(e);
        }
    }

    const disponible = pago ? Number(pago.monto) - Number(pago.montoReembolsado) : 0;

    return (
        <section className="card">
            <h3>Pago {codigo}</h3>
            <ErrorBanner error={error}/>
            {pago && (
                <>
                    <dl className="grid">
                        <dt>Estado</dt><dd><EstadoBadge estado={pago.estado}/></dd>
                        <dt>Monto</dt><dd>{formatoMonto(pago.monto)} {pago.moneda}</dd>
                        <dt>Reembolsado</dt><dd>{formatoMonto(pago.montoReembolsado)}</dd>
                        <dt>Pedido</dt><dd title={pago.codigoPedido}>{corto(pago.codigoPedido)}…</dd>
                        <dt>Usuario</dt><dd>{nombreUsuario(pago.codigoUsuario)}</dd>
                        <dt>Medio de pago</dt>
                        <dd>{pago.medioPago.tipo} · {pago.medioPago.detalleEnmascarado}</dd>
                        <dt>Clave de control</dt><dd><code>{pago.claveControl}</code></dd>
                        <dt>Creado</dt><dd>{formatoFecha(pago.fechaCreacion)}</dd>
                        <dt>Actualizado</dt><dd>{formatoFecha(pago.fechaActualizacion)}</dd>
                    </dl>

                    {puedeOperar && pago.estado === 'PENDIENTE' && (
                        <div className="actions">
                            <button className="primary" onClick={() => ejecutar(() => aprobarPago(token, codigo))}>
                                Aprobar
                            </button>
                            <input placeholder="Motivo del rechazo (opcional)" value={motivoRechazo}
                                   onChange={(e) => setMotivoRechazo(e.target.value)}/>
                            <button className="danger"
                                    onClick={() => ejecutar(() => rechazarPago(token, codigo, motivoRechazo))}>
                                Rechazar
                            </button>
                        </div>
                    )}

                    {puedeOperar && pago.estado === 'APROBADO' && (
                        <form className="actions" onSubmit={(e) => {
                            e.preventDefault();
                            ejecutar(async () => {
                                await reembolsarPago(token, codigo, Number(montoReembolso), motivoReembolso);
                                setMontoReembolso('');
                                setMotivoReembolso('');
                            });
                        }}>
                            <input type="number" step="0.01" min="0.01" placeholder="Monto a reembolsar" required
                                   value={montoReembolso} onChange={(e) => setMontoReembolso(e.target.value)}/>
                            <button type="button" className="chip"
                                    onClick={() => setMontoReembolso(disponible.toFixed(2))}>
                                Total disponible ({formatoMonto(disponible)})
                            </button>
                            <input placeholder="Motivo" required value={motivoReembolso}
                                   onChange={(e) => setMotivoReembolso(e.target.value)}/>
                            <button className="danger">Reembolsar</button>
                        </form>
                    )}

                    {puedeVerReembolsos && reembolsos.length > 0 && (
                        <>
                            <h4>Reembolsos</h4>
                            <table>
                                <thead><tr><th>Código</th><th>Monto</th><th>Motivo</th><th>Fecha</th></tr></thead>
                                <tbody>
                                {reembolsos.map((r) => (
                                    <tr key={r.codigo}>
                                        <td title={r.codigo}>{corto(r.codigo)}…</td>
                                        <td>{formatoMonto(r.monto)}</td>
                                        <td>{r.motivo}</td>
                                        <td>{formatoFecha(r.fecha)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </>
                    )}

                    {puedeAuditar && (
                        <>
                            <h4>Historial de estados</h4>
                            <ol className="timeline">
                                {historial.map((h, i) => (
                                    <li key={i}>
                                        {h.estadoAnterior ? <EstadoBadge estado={h.estadoAnterior}/> : <em>alta</em>}
                                        {' → '}<EstadoBadge estado={h.estadoNuevo}/>
                                        <span className="muted"> · {h.motivo} · {formatoFecha(h.fecha)}</span>
                                    </li>
                                ))}
                            </ol>
                        </>
                    )}
                </>
            )}
        </section>
    );
}
