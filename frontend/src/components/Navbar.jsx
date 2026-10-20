import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { rutasPermitidas } from '../utils/rutas.js';

export default function Navbar() {
  const { authenticated, roles, username, cerrarSesion } = useAuth();
  const navigate = useNavigate();

  function salir() { cerrarSesion(); navigate('/inicio'); }

  return <header className="nav"><div className="brand">DeliveryGo <span>· Pagos</span></div>
    <nav aria-label="Navegación principal">
      <NavLink to="/inicio">Inicio</NavLink>
      {authenticated && rutasPermitidas(roles).map(r => <NavLink key={r.path} to={r.path}>{r.titulo}</NavLink>)}
      {!authenticated
        ? <NavLink to="/login">Iniciar sesión</NavLink>
        : <><span className="user">{username} · {roles.join(', ') || 'sin rol'}</span>
          <button className="ghost" onClick={salir}>Cerrar sesión</button></>}
    </nav></header>;
}
