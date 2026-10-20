import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { ESTADOS_PAGO, TIPOS_MEDIO_PAGO } from '../data/demo.js';
import { rutaInicial } from '../utils/rutas.js';
import EstadoBadge from '../components/EstadoBadge.jsx';

// Página pública: no consulta la API ni envía JWT. Explica el módulo (todo lo demás es privado).
export default function InicioPage() {
  const { authenticated, roles } = useAuth();
  return <section className="panel"><p className="eyebrow">Acceso público · sin JWT</p><h1>Módulo de Pagos</h1>
    <p>DeliveryGo registra los intentos de pago de cada pedido, los autoriza o rechaza de forma <strong>simulada</strong> y
      gestiona reembolsos totales o parciales. No se procesa dinero real ni se guardan datos reales de tarjetas.</p>
    <div className="cards">
      <article className="item"><h3>Medios de pago aceptados</h3>
        <ul>{TIPOS_MEDIO_PAGO.map(t => <li key={t}><code>{t}</code></li>)}</ul>
        <p className="hint">Solo se guardan los últimos 4 dígitos.</p></article>
      <article className="item"><h3>Estados de un pago</h3>
        <p>{ESTADOS_PAGO.map(e => <span key={e}><EstadoBadge estado={e}/>{' '}</span>)}</p>
        <p className="hint">PENDIENTE → APROBADO o RECHAZADO. APROBADO → REEMBOLSADO cuando se devuelve el total.</p></article>
      <article className="item"><h3>Reglas clave</h3>
        <ul><li>La misma clave de control no crea dos pagos.</li><li>El importe debe coincidir con el total del pedido.</li>
          <li>Un pago rechazado se reintenta con una clave nueva.</li><li>Los reembolsos no superan lo aprobado.</li></ul></article>
    </div>
    <p>{authenticated
      ? <Link to={rutaInicial(roles)}>Ir a mi área de trabajo</Link>
      : <>Para operar con pagos, <Link to="/login">iniciá sesión</Link>.</>}</p></section>;
}
