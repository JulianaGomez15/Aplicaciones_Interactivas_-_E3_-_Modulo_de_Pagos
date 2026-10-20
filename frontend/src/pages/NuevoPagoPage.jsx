import { useMemo, useState } from 'react';
import { iniciarPago } from '../services/pagoService.js';
import { useAuth } from '../context/AuthContext.jsx';
import { formatoMonto, PEDIDOS, USUARIOS } from '../data/demo.js';
import { leerMedios } from '../data/mediosGuardados.js';
import PagoDetalle from '../components/PagoDetalle.jsx';

// CU-01 / CU-09: lo que normalmente envía el Servicio de Pedidos. Rol SERVICIO_ESCRITURA.
export default function NuevoPagoPage() {
  const { token } = useAuth();
  const [codigoPedido, setCodigoPedido] = useState(PEDIDOS[0].codigo);
  const [codigoUsuario, setCodigoUsuario] = useState(USUARIOS[0].codigo);
  const [codigoMedioPago, setCodigoMedioPago] = useState('');
  const [monto, setMonto] = useState(String(PEDIDOS[0].total));
  const [moneda, setMoneda] = useState('ARS');
  const [claveControl, setClaveControl] = useState(() => crypto.randomUUID());
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const mediosConocidos = useMemo(() => leerMedios(), []);

  function elegirPedido(codigo) {
    setCodigoPedido(codigo);
    setMonto(String(PEDIDOS.find(p => p.codigo === codigo).total));
  }

  async function enviar(e) {
    e.preventDefault(); setLoading(true); setError('');
    try {
      const { status, data } = await iniciarPago(token, {
        codigoPedido, codigoUsuario, codigoMedioPago, monto: Number(monto), moneda, claveControl
      });
      setResultado({ status, pago: data });
    } catch (err) { setResultado(null); setError(err.message); }
    finally { setLoading(false); }
  }

  return <>
    <section className="panel"><p className="eyebrow">Área privada · SERVICIO_ESCRITURA</p><h1>Iniciar pago</h1>
      <p>Envía <code>POST /api/v1/payments</code>. El backend valida usuario, medio de pago, pedido y monto.</p>
      <form onSubmit={enviar} className="form grid2">
        <label>Pedido<select value={codigoPedido} onChange={e => elegirPedido(e.target.value)}>
          {PEDIDOS.map(p => <option key={p.codigo} value={p.codigo}>…{p.codigo.slice(-1)} · {formatoMonto(p.total)} · {p.estado}</option>)}
        </select></label>
        <label>Usuario<select value={codigoUsuario} onChange={e => setCodigoUsuario(e.target.value)}>
          {USUARIOS.map(u => <option key={u.codigo} value={u.codigo}>{u.nombre}</option>)}
        </select></label>
        <label>Medio de pago (código)
          <input list="medios-conocidos" placeholder="UUID del medio de pago" required value={codigoMedioPago} onChange={e => setCodigoMedioPago(e.target.value)} />
          <datalist id="medios-conocidos">{mediosConocidos.map(m => <option key={m.codigoMedioPago} value={m.codigoMedioPago}>
            {m.tipo} · {m.detalleEnmascarado} (de {m.registradoPor})</option>)}</datalist></label>
        <label>Monto<input type="number" step="0.01" value={monto} onChange={e => setMonto(e.target.value)} required /></label>
        <label>Moneda<input maxLength={3} value={moneda} onChange={e => setMoneda(e.target.value.toUpperCase())} required /></label>
        <label className="wide">Clave de control (idempotencia)
          <span className="inline"><input value={claveControl} onChange={e => setClaveControl(e.target.value)} required />
            <button type="button" className="secondary" onClick={() => setClaveControl(crypto.randomUUID())}>Nueva clave</button></span></label>
        <div className="wide"><button disabled={loading} type="submit">{loading ? 'Enviando…' : 'Enviar solicitud de pago'}</button></div>
      </form>
      <p className="hint">El medio de pago lo registra un cliente (cuenta <code>cliente</code> o <code>cliente2</code>) y debe ser del usuario elegido.
        Reenviar con la misma clave devuelve el pago original (200); con otro monto, <code>DUPLICATE_PAYMENT_REQUEST</code>;
        un monto distinto al del pedido, <code>PAYMENT_AMOUNT_MISMATCH</code>.</p>
      {error && <p role="alert" className="error">{error}</p>}
      {resultado && <p role="status" className="ok">{resultado.status === 201
        ? 'Pago creado (201 Created).'
        : 'La clave de control ya existía: se devolvió el pago original (200 OK).'}</p>}
    </section>
    {resultado && <PagoDetalle key={resultado.pago.codigoPago} codigoPago={resultado.pago.codigoPago} />}
  </>;
}
