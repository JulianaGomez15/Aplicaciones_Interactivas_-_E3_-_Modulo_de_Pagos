import {useEffect, useState} from 'react';
import {iniciarPago, listarMediosPago} from '../services/pagosService.js';
import {formatoMonto, PEDIDOS, USUARIOS} from '../data/demo.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import PagoDetalle from '../components/PagoDetalle.jsx';

// CU-01 / CU-09: lo que normalmente enviaria el Servicio de Pedidos.
export default function NuevoPagoPage({session}) {
    const [codigoPedido, setCodigoPedido] = useState(PEDIDOS[0].codigo);
    const [codigoUsuario, setCodigoUsuario] = useState(USUARIOS[0].codigo);
    const [medios, setMedios] = useState([]);
    const [codigoMedioPago, setCodigoMedioPago] = useState('');
    const [monto, setMonto] = useState(String(PEDIDOS[0].total));
    const [claveControl, setClaveControl] = useState(() => crypto.randomUUID());
    const [resultado, setResultado] = useState(null);
    const [error, setError] = useState(null);

    // Solo ADMIN y CLIENTE pueden listar medios de pago; para otros roles se pega el codigo a mano.
    useEffect(() => {
        let vigente = true;
        listarMediosPago(session.token, codigoUsuario)
            .then((lista) => {
                if (!vigente) return;
                setMedios(lista);
                setCodigoMedioPago(lista[0]?.codigo ?? '');
            })
            .catch(() => {
                if (!vigente) return;
                setMedios([]);
                setCodigoMedioPago('');
            });
        return () => {
            vigente = false;
        };
    }, [session.token, codigoUsuario]);

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
                    <label>Medio de pago
                        {medios.length > 0 ? (
                            <select value={codigoMedioPago} onChange={(e) => setCodigoMedioPago(e.target.value)}>
                                {medios.map((m) => (
                                    <option key={m.codigo} value={m.codigo}>
                                        {m.tipo} · {m.detalleEnmascarado}
                                    </option>
                                ))}
                            </select>
                        ) : (
                            <input placeholder="código de medio de pago (UUID)" value={codigoMedioPago}
                                   onChange={(e) => setCodigoMedioPago(e.target.value)}/>
                        )}
                    </label>
                    <label>Monto (ARS)
                        <input type="number" step="0.01" value={monto} onChange={(e) => setMonto(e.target.value)}/>
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
                    Reenviar con la misma clave devuelve el pago original (200). Cambiar el monto con la misma
                    clave da DUPLICATE_PAYMENT_REQUEST; un monto distinto al del pedido, PAYMENT_AMOUNT_MISMATCH.
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
            {resultado && <PagoDetalle key={resultado.pago.codigo} session={session} codigo={resultado.pago.codigo}/>}
        </>
    );
}
