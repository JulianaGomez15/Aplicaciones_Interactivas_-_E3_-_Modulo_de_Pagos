import { useCallback, useEffect, useState } from 'react';
import { aprobarPago, consultarPago, historialPago, rechazarPago, reembolsarPago } from '../services/pagoService.js';
import { useAuth } from '../context/AuthContext.jsx';
import { corto, formatoFecha, formatoMonto } from '../data/demo.js';
import EstadoBadge from './EstadoBadge.jsx';

// Detalle de un pago + acciones según el rol. Los permisos reales los valida el backend en cada llamada.
export default function PagoDetalle({ codigoPago, onChanged }) {
  const { token, tieneRol } = useAuth();
  const puedeOperar = tieneRol('SERVICIO_ESCRITURA');
  const puedeReembolsar = tieneRol('SERVICIO_ESCRITURA', 'ADMINISTRADOR');
  const puedeVerHistorial = tieneRol('ADMINISTRADOR');

  const [pago, setPago] = useState(null);
  const [historial, setHistorial] = useState([]);
  const [reembolsos, setReembolsos] = useState([]); // los de esta sesión: la API no los lista
  const [aviso, setAviso] = useState('');
  const [error, setError] = useState('');
  const [motivoRechazo, setMotivoRechazo] = useState('');
  const [monto, setMonto] = useState('');
  const [motivo, setMotivo] = useState('');
  const [clave, setClave] = useState(() => crypto.randomUUID());
  const [busy, setBusy] = useState(false);

  const cargar = useCallback(async () => {
    try {
      setPago(await consultarPago(token, codigoPago));
      setHistorial(puedeVerHistorial ? await historialPago(token, codigoPago) : []);
      setError('');
    } catch (err) { setPago(null); setError(err.message); }
  }, [token, codigoPago, puedeVerHistorial]);

  useEffect(() => { cargar(); }, [cargar]);

  async function ejecutar(accion) {
    setBusy(true); setAviso(''); setError('');
    try { await accion(); await cargar(); onChanged?.(); }
    catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }

  function reembolsar(e) {
    e.preventDefault();
    return ejecutar(async () => {
      const { status, data } = await reembolsarPago(token, codigoPago, Number(monto), motivo, clave);
      setReembolsos(lista => [data, ...lista.filter(r => r.codigoReembolso !== data.codigoReembolso)]);
      setAviso(status === 201
        ? `Reembolso registrado. Disponible para reembolsar: ${formatoMonto(data.montoDisponible)}.`
        : 'La clave de control ya se había usado: se devolvió el reembolso existente (200 OK).');
      setMonto(''); setMotivo(''); setClave(crypto.randomUUID());
    });
  }

  // Lo que queda por devolver: se conoce con el último reembolso de la sesión; si no, es el monto completo.
  const disponible = reembolsos.length > 0 ? Number(reembolsos[0].montoDisponible) : Number(pago?.monto ?? 0);

  return <section className="panel detalle"><p className="eyebrow">Pago</p><h3>{codigoPago}</h3>
    {error && <p role="alert" className="error">{error}</p>}
    {aviso && <p role="status" className="ok">{aviso}</p>}
    {pago && <>
      <dl className="grid">
        <dt>Estado</dt><dd><EstadoBadge estado={pago.estado}/></dd>
        <dt>Monto</dt><dd>{formatoMonto(pago.monto)} {pago.moneda}</dd>
        <dt>Pedido</dt><dd title={pago.codigoPedido}>{corto(pago.codigoPedido)}</dd>
        <dt>Medio de pago</dt><dd>{pago.medioPago.tipo} · {pago.medioPago.detalleEnmascarado}</dd>
        <dt>Creado</dt><dd>{formatoFecha(pago.fechaCreacion)}</dd>
        <dt>Actualizado</dt><dd>{formatoFecha(pago.fechaActualizacion)}</dd>
      </dl>

      {puedeOperar && pago.estado === 'PENDIENTE' && <div className="actions">
        <button disabled={busy} onClick={() => ejecutar(() => aprobarPago(token, codigoPago))}>Aprobar</button>
        <input placeholder="Motivo del rechazo (opcional)" value={motivoRechazo} onChange={e => setMotivoRechazo(e.target.value)} />
        <button className="danger" disabled={busy} onClick={() => ejecutar(() => rechazarPago(token, codigoPago, motivoRechazo))}>Rechazar</button>
      </div>}

      {puedeReembolsar && pago.estado === 'APROBADO' && <form className="actions" onSubmit={reembolsar}>
        <input type="number" step="0.01" min="0.01" placeholder="Monto a reembolsar" required value={monto} onChange={e => setMonto(e.target.value)} />
        <button type="button" className="secondary" onClick={() => setMonto(disponible.toFixed(2))}>Total disponible ({formatoMonto(disponible)})</button>
        <input placeholder="Motivo" required value={motivo} onChange={e => setMotivo(e.target.value)} />
        <input title="Clave de control del reembolso (idempotencia)" required value={clave} onChange={e => setClave(e.target.value)} />
        <button className="danger" disabled={busy}>Reembolsar</button>
      </form>}

      {reembolsos.length > 0 && <><h4>Reembolsos de esta sesión</h4>
        <table><thead><tr><th>Código</th><th>Monto</th><th>Estado</th><th>Fecha</th></tr></thead><tbody>
          {reembolsos.map(r => <tr key={r.codigoReembolso}><td title={r.codigoReembolso}>{corto(r.codigoReembolso)}</td>
            <td>{formatoMonto(r.monto)}</td><td>{r.estado}</td><td>{formatoFecha(r.fechaResolucion)}</td></tr>)}
        </tbody></table></>}

      {puedeVerHistorial && <><h4>Historial de estados</h4>
        <ol className="timeline">{historial.map((h, i) => <li key={i}>
          {h.estadoAnterior ? <EstadoBadge estado={h.estadoAnterior}/> : <em>alta</em>}{' → '}<EstadoBadge estado={h.estadoNuevo}/>
          <span className="hint"> · {h.motivo} · {formatoFecha(h.fecha)}</span></li>)}</ol></>}
    </>}
  </section>;
}
