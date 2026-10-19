import {useState} from 'react';
import {registrarMedioPago} from '../services/pagosService.js';
import {leerMedios, recordarMedio} from '../data/mediosGuardados.js';
import {TIPOS_MEDIO_PAGO} from '../data/demo.js';
import ErrorBanner from '../components/ErrorBanner.jsx';

// CU-08: el numero se envia en claro una sola vez y el backend guarda solo la version enmascarada.
// El usuario dueño del medio es el del JWT (no se elige).
export default function MediosPagoPage({session}) {
    const [tipo, setTipo] = useState(TIPOS_MEDIO_PAGO[0]);
    const [numero, setNumero] = useState('4111 1111 1111 1111');
    const [medios, setMedios] = useState(() => leerMedios().filter((m) => m.registradoPor === session.username));
    const [error, setError] = useState(null);

    async function crear(e) {
        e.preventDefault();
        try {
            const medio = await registrarMedioPago(session.token, tipo, numero);
            recordarMedio(medio, session.username);
            setMedios((lista) => [{...medio, registradoPor: session.username}, ...lista]);
            setNumero('');
            setError(null);
        } catch (err) {
            setError(err);
        }
    }

    return (
        <section className="card">
            <h2>Medios de pago</h2>
            <form onSubmit={crear} className="form-grid">
                <label>Tipo
                    <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
                        {TIPOS_MEDIO_PAGO.map((t) => <option key={t}>{t}</option>)}
                    </select>
                </label>
                <label>Número (tarjeta de 13 a 19 dígitos, o alias simulado)
                    <input value={numero} onChange={(e) => setNumero(e.target.value)}/>
                </label>
                <div className="wide"><button className="primary">Registrar medio de pago</button></div>
            </form>
            <p className="muted">
                Se guarda solo el detalle enmascarado (últimos 4 dígitos); el número completo se descarta. Copiá el
                código del medio para usarlo en «Iniciar pago».
            </p>
            <ErrorBanner error={error}/>
            <table>
                <thead><tr><th>Tipo</th><th>Detalle</th><th>Código</th></tr></thead>
                <tbody>
                {medios.map((m) => (
                    <tr key={m.codigoMedioPago}>
                        <td>{m.tipo}</td>
                        <td><code>{m.detalleEnmascarado}</code></td>
                        <td><small>{m.codigoMedioPago}</small></td>
                    </tr>
                ))}
                {medios.length === 0 && (
                    <tr><td colSpan="3" className="muted">Todavía no registraste medios de pago en este navegador</td></tr>
                )}
                </tbody>
            </table>
        </section>
    );
}
