import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { tieneAlgunRol } from '../utils/jwt.js';

// roles: alcanza con tener uno de ellos.
export default function ProtectedRoute({ children, roles = [] }) {
  const { authenticated, roles: rolesUsuario } = useAuth();
  const location = useLocation();
  if (!authenticated) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (!tieneAlgunRol(rolesUsuario, roles)) return <Navigate to="/no-autorizado" replace />;
  return children;
}
