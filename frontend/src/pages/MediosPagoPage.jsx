import {useCallback, useEffect, useState} from 'react';
import {crearMedioPago, listarMediosPago} from '../services/pagosService.js';
import {formatoFecha, TIPOS_MEDIO_PAGO, USUARIOS} from '../data/demo.js';
import ErrorBanner from '../components/ErrorBanner.jsx';

// CU-08: el detalle se envia en claro una sola vez y el backend solo guarda la version enmascarada.
export default function MediosPagoPage({session}) {
    const [codigoUsuario, setCodigoUsuario] = useState(USUARIOS[0].codigo);
    const [tipo, setTipo] = useState(TIPOS_MEDIO_PAGO[0]);
    const [detalle, setDetalle] = useState('4111 1111 1111 1111');
    const [medios, setMedios] = useState([]);
    const [error, setError] = useState(null);

    const cargar = useCallback(async () => {
        try {
            setMedios(await listarMediosPago(session.token, codigoUsuario));
            setError(null);
        } catch (e) {
            setError(e);
        }
    }, [session.token, codigoUsuario]);

    useEffect(() => {
        cargar();
    }, [cargar]);

    async function crear(e) {
        e.preventDefault();
        try {
            await crearMedioPago(session.token, codigoUsuario, tipo, detalle);
            setDetalle('');
            await cargar();
        } catch (err) {
            setError(err);
        }
    }

    return (
        <section className="card">
            <h2>Medios de pago</h2>
            <div className="form-grid">
                <label>Usuario
                    <select value={codigoUsuario} onChange={(e) => setCodigoUsuario(e.target.value)}>
                        {USUARIOS.map((u) => <option key={u.codigo} value={u.codigo}>{u.nombre}</option>)}
                    </select>
                </label>
            </div>
            <form onSubmit={crear} className="form-grid">
                <label>Tipo
                    <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
                        {TIPOS_MEDIO_PAGO.map((t) => <option key={t}>{t}</option>)}
                    </select>
                </label>
                <label>Detalle (nº de tarjeta o alias simulado)
                    <input value={detalle} onChange={(e) => setDetalle(e.target.value)}/>
                </label>
                <div className="wide"><button className="primary">Registrar medio de pago</button></div>
            </form>
            <p className="muted">Solo se guarda el detalle enmascarado (últimos 4 dígitos); nunca el dato completo.</p>
            <ErrorBanner error={error}/>
            <table>
                <thead><tr><th>Tipo</th><th>Detalle</th><th>Alta</th><th>Código</th></tr></thead>
                <tbody>
                {medios.map((m) => (
                    <tr key={m.codigo}>
                        <td>{m.tipo}</td>
                        <td><code>{m.detalleEnmascarado}</code></td>
                        <td>{formatoFecha(m.fechaCreacion)}</td>
                        <td title={m.codigo}><small>{m.codigo}</small></td>
                    </tr>
                ))}
                {medios.length === 0 && <tr><td colSpan="4" className="muted">Sin medios de pago</td></tr>}
                </tbody>
            </table>
        </section>
    );
}
