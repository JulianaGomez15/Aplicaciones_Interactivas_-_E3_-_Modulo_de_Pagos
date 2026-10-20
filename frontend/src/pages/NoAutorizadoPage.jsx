import { Link } from 'react-router-dom';

export default function NoAutorizadoPage() {
  return <section className="panel compact"><p className="eyebrow">403 · Navegación</p><h1>Acceso no autorizado</h1>
    <p>Tu usuario no tiene el rol requerido para acceder a esta página.</p><Link to="/inicio">Volver al inicio</Link></section>;
}
