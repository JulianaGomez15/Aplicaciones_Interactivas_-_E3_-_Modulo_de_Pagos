import {useCallback, useEffect, useState} from 'react';
import {aprobarPago, historialPago, obtenerPago, rechazarPago, reembolsarPago} from '../services/pagosService.js';
import {corto, formatoFecha, formatoMonto, tieneRol} from '../data/demo.js';
import ErrorBanner from './ErrorBanner.jsx';

export function EstadoBadge({estado}) {
    return <span className={`badge ${estado}`}>{estado}</span>;
}

// Detalle de un pago + acciones segun el rol (los permisos reales los valida el backend).
export default function PagoDetalle({session, codigoPago, onChanged}) {
    const {token} = session;
    const puedeOperar = tieneRol(session, 'SERVICIO_ESCRITURA');
    const puedeReembolsar = tieneRol(session, 'SERVICIO_ESCRITURA', 'ADMINISTRADOR');
    const puedeVerHistorial = tieneRol(session, 'ADMINISTRADOR');

    const [pago, setPago] = useState(null);
    const [historial, setHistorial] = useState([]);
    const [reembolsos, setReembolsos] = useState([]); // los de esta sesion: la API no los lista
    const [aviso, setAviso] = useState(null);
    const [error, setError] = useState(null);
    const [motivoRechazo, setMotivoRechazo] = useState('');
    const [montoReembolso, setMontoReembolso] = useState('');
    const [motivoReembolso, setMotivoReembolso] = useState('');
    const [claveReembolso, setClaveReembolso] = useState(() => crypto.randomUUID());

    const cargar = useCallback(async () => {
        try {
            setPago(await obtenerPago(token, codigoPago));
            setHistorial(puedeVerHistorial ? await historialPago(token, codigoPago) : []);
            setError(null);
        } catch (e) {
            setPago(null);
            setError(e);
        }
    }, [token, codigoPago, puedeVerHistorial]);

    useEffect(() => {
        cargar();
    }, [cargar]);

    async function ejecutar(accion) {
        try {
            setAviso(null);
            await accion();
            setError(null);
            await cargar();
            onChanged?.();
        } catch (e) {
            setError(e);
        }
    }

    async function reembolsar(e) {
        e.preventDefault();
        await ejecutar(async () => {
            const r = await reembolsarPago(token, codigoPago, Number(montoReembolso), motivoReembolso, claveReembolso);
            setReembolsos((lista) => [r.data, ...lista.filter((x) => x.codigoReembolso !== r.data.codigoReembolso)]);
            setAviso(r.status === 201
                ? `Reembolso registrado. Disponible para reembolsar: ${formatoMonto(r.data.montoDisponible)}.`
                : 'La clave de control ya se había usado: se devolvió el reembolso existente (200 OK).');
            setMontoReembolso('');
            setMotivoReembolso('');
            setClaveReembolso(crypto.randomUUID());
        });
    }

    // Lo que queda por devolver: se conoce con el ultimo reembolso de la sesion; si no, es el monto completo.
    const disponible = reembolsos.length > 0 ? Number(reembolsos[0].montoDisponible) : pago ? Number(pago.monto) : 0;

    return (
        <section className="card">
            <h3>Pago {codigoPago}</h3>
            <ErrorBanner error={error}/>
            {aviso && <div className="banner ok">{aviso}</div>}
            {pago && (
                <>
                    <dl className="grid">
                        <dt>Estado</dt><dd><EstadoBadge estado={pago.estado}/></dd>
                        <dt>Monto</dt><dd>{formatoMonto(pago.monto)} {pago.moneda}</dd>
                        <dt>Pedido</dt><dd title={pago.codigoPedido}>{corto(pago.codigoPedido)}…</dd>
                        <dt>Medio de pago</dt>
                        <dd>{pago.medioPago.tipo} · {pago.medioPago.detalleEnmascarado}</dd>
                        <dt>Creado</dt><dd>{formatoFecha(pago.fechaCreacion)}</dd>
                        <dt>Actualizado</dt><dd>{formatoFecha(pago.fechaActualizacion)}</dd>
                    </dl>

                    {puedeOperar && pago.estado === 'PENDIENTE' && (
                        <div className="actions">
                            <button className="primary" onClick={() => ejecutar(() => aprobarPago(token, codigoPago))}>
                                Aprobar
                            </button>
                            <input placeholder="Motivo del rechazo (opcional)" value={motivoRechazo}
                                   onChange={(e) => setMotivoRechazo(e.target.value)}/>
                            <button className="danger"
                                    onClick={() => ejecutar(() => rechazarPago(token, codigoPago, motivoRechazo))}>
                                Rechazar
                            </button>
                        </div>
                    )}

                    {puedeReembolsar && pago.estado === 'APROBADO' && (
                        <form className="actions" onSubmit={reembolsar}>
                            <input type="number" step="0.01" min="0.01" placeholder="Monto a reembolsar" required
                                   value={montoReembolso} onChange={(e) => setMontoReembolso(e.target.value)}/>
                            <button type="button" className="chip"
                                    onClick={() => setMontoReembolso(disponible.toFixed(2))}>
                                Total disponible ({formatoMonto(disponible)})
                            </button>
                            <input placeholder="Motivo" required value={motivoReembolso}
                                   onChange={(e) => setMotivoReembolso(e.target.value)}/>
                            <input className="clave" title="Clave de control del reembolso (idempotencia)"
                                   value={claveReembolso} onChange={(e) => setClaveReembolso(e.target.value)}/>
                            <button className="danger">Reembolsar</button>
                        </form>
                    )}

                    {reembolsos.length > 0 && (
                        <>
                            <h4>Reembolsos de esta sesión</h4>
                            <table>
                                <thead><tr><th>Código</th><th>Monto</th><th>Estado</th><th>Fecha</th></tr></thead>
                                <tbody>
                                {reembolsos.map((r) => (
                                    <tr key={r.codigoReembolso}>
                                        <td title={r.codigoReembolso}>{corto(r.codigoReembolso)}…</td>
                                        <td>{formatoMonto(r.monto)}</td>
                                        <td>{r.estado}</td>
                                        <td>{formatoFecha(r.fechaResolucion)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </>
                    )}

                    {puedeVerHistorial && (
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
