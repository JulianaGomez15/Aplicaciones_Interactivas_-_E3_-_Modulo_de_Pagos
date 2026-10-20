import { useState } from 'react';
import { listarPagos } from '../services/pagoService.js';
import { useAuth } from '../context/AuthContext.jsx';
import { corto, formatoMonto, PEDIDOS } from '../data/demo.js';
import EstadoBadge from '../components/EstadoBadge.jsx';
import PagoDetalle from '../components/PagoDetalle.jsx';

// CU-04: por código de pago (un CLIENTE solo ve los suyos). CU-07: por pedido, si el rol puede listar.
export default function ConsultaPagoPage() {
  const { token, tieneRol } = useAuth();
  const puedePorPedido = tieneRol('ADMINISTRADOR', 'SERVICIO_LECTURA');
  const [codigoPago, setCodigoPago] = useState('');
  const [consultado, setConsultado] = useState('');
  const [codigoPedido, setCodigoPedido] = useState(PEDIDOS[0].codigo);
  const [pagosPedido, setPagosPedido] = useState(null);
  const [error, setError] = useState('');

  async function buscarPorPedido(e) {
    e.preventDefault(); setError('');
    try { setPagosPedido((await listarPagos(token, { codigoPedido, size: 50, sort: 'fechaCreacion,desc' })).content); }
    catch (err) { setPagosPedido(null); setError(err.message); }
  }

  return <>
    <section className="panel"><p className="eyebrow">Área privada · CLIENTE, ADMINISTRADOR, SERVICIO_LECTURA</p><h1>Consultar pago</h1>
      <form className="inline" onSubmit={e => { e.preventDefault(); setConsultado(codigoPago.trim()); }}>
        <input className="grow" placeholder="Código de pago (UUID)" value={codigoPago} onChange={e => setCodigoPago(e.target.value)} required />
        <button type="submit">Buscar</button></form>
      <p className="hint">Un cliente solo puede ver sus propios pagos: si el pago es de otro usuario la API responde 403.</p>

      {puedePorPedido && <><h3>Intentos de pago de un pedido</h3>
        <form className="inline" onSubmit={buscarPorPedido}>
          <select className="grow" value={codigoPedido} onChange={e => setCodigoPedido(e.target.value)}>
            {PEDIDOS.map(p => <option key={p.codigo} value={p.codigo}>…{p.codigo.slice(-1)} · {formatoMonto(p.total)} · {p.estado}</option>)}</select>
          <button type="submit">Buscar</button></form>
        {pagosPedido && <table><thead><tr><th>Código</th><th>Monto</th><th>Estado</th><th/></tr></thead><tbody>
          {pagosPedido.map(p => <tr key={p.codigoPago}><td title={p.codigoPago}>{corto(p.codigoPago)}</td><td>{formatoMonto(p.monto)}</td>
            <td><EstadoBadge estado={p.estado}/></td><td><button className="secondary" onClick={() => setConsultado(p.codigoPago)}>Ver</button></td></tr>)}
          {pagosPedido.length === 0 && <tr><td colSpan="4">El pedido no tiene pagos.</td></tr>}</tbody></table>}</>}
      {error && <p role="alert" className="error">{error}</p>}
    </section>
    {consultado && <PagoDetalle key={consultado} codigoPago={consultado} />}
  </>;
}
