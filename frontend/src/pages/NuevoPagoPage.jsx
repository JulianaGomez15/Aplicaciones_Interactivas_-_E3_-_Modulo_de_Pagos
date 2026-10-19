import {useMemo, useState} from 'react';
import {iniciarPago} from '../services/pagosService.js';
import {formatoMonto, PEDIDOS, USUARIOS} from '../data/demo.js';
import {leerMedios} from '../data/mediosGuardados.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import PagoDetalle from '../components/PagoDetalle.jsx';

// CU-01 / CU-09: lo que normalmente enviaria el Servicio de Pedidos.
export default function NuevoPagoPage({session}) {
    const [codigoPedido, setCodigoPedido] = useState(PEDIDOS[0].codigo);
    const [codigoUsuario, setCodigoUsuario] = useState(USUARIOS[0].codigo);
    const [codigoMedioPago, setCodigoMedioPago] = useState('');
    const [monto, setMonto] = useState(String(PEDIDOS[0].total));
    const [moneda, setMoneda] = useState('ARS');
    const [claveControl, setClaveControl] = useState(() => crypto.randomUUID());
    const [resultado, setResultado] = useState(null);
    const [error, setError] = useState(null);

    // Medios que se registraron desde este navegador (la API no los lista).
    const mediosConocidos = useMemo(() => leerMedios(), []);

    function elegirPedido(codigo) {
        setCodigoPedido(codigo);
        setMonto(String(PEDIDOS.find((p) => p.codigo === codigo).total));
    }

    async function enviar(e) {
        e.preventDefault();
        try {
            const r = await iniciarPago(session.token, {
                codigoPedido,
                codigoUsuario,
                codigoMedioPago,
                monto: Number(monto),
                moneda,
                claveControl
            });
            setResultado({status: r.status, pago: r.data});
            setError(null);
        } catch (err) {
            setResultado(null);
            setError(err);
        }
    }

    return (
        <>
            <section className="card">
                <h2>Iniciar pago</h2>
                <form onSubmit={enviar} className="form-grid">
                    <label>Pedido
                        <select value={codigoPedido} onChange={(e) => elegirPedido(e.target.value)}>
                            {PEDIDOS.map((p) => (
                                <option key={p.codigo} value={p.codigo}>
                                    …{p.codigo.slice(-1)} · {formatoMonto(p.total)} · {p.estado}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label>Usuario
                        <select value={codigoUsuario} onChange={(e) => setCodigoUsuario(e.target.value)}>
                            {USUARIOS.map((u) => <option key={u.codigo} value={u.codigo}>{u.nombre}</option>)}
                        </select>
                    </label>
                    <label>Medio de pago (código)
                        <input list="medios-conocidos" placeholder="UUID del medio de pago" required
                               value={codigoMedioPago} onChange={(e) => setCodigoMedioPago(e.target.value)}/>
                        <datalist id="medios-conocidos">
                            {mediosConocidos.map((m) => (
                                <option key={m.codigoMedioPago} value={m.codigoMedioPago}>
                                    {m.tipo} · {m.detalleEnmascarado} (registrado por {m.registradoPor})
                                </option>
                            ))}
                        </datalist>
                    </label>
                    <label>Monto
                        <input type="number" step="0.01" value={monto} onChange={(e) => setMonto(e.target.value)}/>
                    </label>
                    <label>Moneda
                        <input maxLength={3} value={moneda} onChange={(e) => setMoneda(e.target.value.toUpperCase())}/>
                    </label>
                    <label className="wide">Clave de control (idempotencia)
                        <span className="inline">
                            <input value={claveControl} onChange={(e) => setClaveControl(e.target.value)}/>
                            <button type="button" className="chip"
                                    onClick={() => setClaveControl(crypto.randomUUID())}>Nueva clave</button>
                        </span>
                    </label>
                    <div className="wide"><button className="primary">Enviar solicitud de pago</button></div>
                </form>
                <p className="muted">
                    El medio de pago lo registra un cliente (pestaña «Medios de pago», con la cuenta cliente o
                    cliente2) y debe ser del usuario elegido. Reenviar con la misma clave devuelve el pago original
                    (200); cambiar el monto con la misma clave da DUPLICATE_PAYMENT_REQUEST; un monto distinto al
                    del pedido, PAYMENT_AMOUNT_MISMATCH.
                </p>
                <ErrorBanner error={error}/>
                {resultado && (
                    <div className="banner ok">
                        {resultado.status === 201
                            ? 'Pago creado (201 Created).'
                            : 'La clave de control ya existía: se devolvió el pago original (200 OK).'}
                    </div>
                )}
            </section>
            {resultado && (
                <PagoDetalle key={resultado.pago.codigoPago} session={session} codigoPago={resultado.pago.codigoPago}/>
            )}
        </>
    );
}
