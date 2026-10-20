import { useState } from 'react';
import { registrarMedioPago } from '../services/medioPagoService.js';
import { useAuth } from '../context/AuthContext.jsx';
import { leerMedios, recordarMedio } from '../data/mediosGuardados.js';
import { TIPOS_MEDIO_PAGO } from '../data/demo.js';

// CU-08: el número se envía en claro una sola vez y el backend guarda solo la versión enmascarada.
// El usuario dueño del medio es el del JWT (no se elige).
export default function MediosPagoPage() {
  const { token, username } = useAuth();
  const [tipo, setTipo] = useState(TIPOS_MEDIO_PAGO[0]);
  const [numero, setNumero] = useState('4111 1111 1111 1111');
  const [medios, setMedios] = useState(() => leerMedios().filter(m => m.registradoPor === username));
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function crear(e) {
    e.preventDefault(); setLoading(true); setError('');
    try {
      const medio = await registrarMedioPago(token, tipo, numero);
      recordarMedio(medio, username);
      setMedios(lista => [{ ...medio, registradoPor: username }, ...lista]);
      setNumero('');
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }

  return <section className="panel"><p className="eyebrow">Área privada · CLIENTE</p><h1>Mis medios de pago</h1>
    <form onSubmit={crear} className="form grid2">
      <label>Tipo<select value={tipo} onChange={e => setTipo(e.target.value)}>{TIPOS_MEDIO_PAGO.map(t => <option key={t}>{t}</option>)}</select></label>
      <label>Número (tarjeta de 13 a 19 dígitos, o alias simulado)<input value={numero} onChange={e => setNumero(e.target.value)} required /></label>
      <div className="wide"><button disabled={loading} type="submit">{loading ? 'Registrando…' : 'Registrar medio de pago'}</button></div>
    </form>
    <p className="hint">Se guarda solo el detalle enmascarado (últimos 4 dígitos); el número completo se descarta.
      La API no lista medios de pago: acá se muestran los que registraste desde este navegador. Copiá el código para usarlo en «Iniciar pago».</p>
    {error && <p role="alert" className="error">{error}</p>}
    <table><thead><tr><th>Tipo</th><th>Detalle</th><th>Código</th></tr></thead><tbody>
      {medios.map(m => <tr key={m.codigoMedioPago}><td>{m.tipo}</td><td><code>{m.detalleEnmascarado}</code></td><td><small>{m.codigoMedioPago}</small></td></tr>)}
      {medios.length === 0 && <tr><td colSpan="3">Todavía no registraste medios de pago en este navegador.</td></tr>}
    </tbody></table></section>;
}
