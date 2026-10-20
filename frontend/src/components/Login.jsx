import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { login } from '../services/authService.js';
import { useAuth } from '../context/AuthContext.jsx';
import { leerClaims, rolesDelToken } from '../utils/jwt.js';
import { rutaInicial } from '../utils/rutas.js';
import { CUENTAS } from '../data/demo.js';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  async function entrar(e) {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const data = await login(username, password);
      iniciarSesion(data);
      const roles = rolesDelToken(leerClaims(data.token));
      navigate(location.state?.from || rutaInicial(roles), { replace: true });
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }

  return <section className="panel compact"><p className="eyebrow">Acceso a DeliveryGo</p><h1>Iniciar sesión</h1>
    <p>Cuentas de demo (cada rol ve y puede hacer solo lo que le corresponde):</p>
    <div className="chips">{CUENTAS.map(c => <button type="button" key={c.username} title={c.descripcion}
      className="secondary" onClick={() => { setUsername(c.username); setPassword(c.password); }}>{c.username}</button>)}</div>
    <form onSubmit={entrar} className="form">
      <label>Usuario<input autoComplete="username" value={username} onChange={e => setUsername(e.target.value)} required /></label>
      <label>Contraseña<input type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} required /></label>
      <button disabled={loading} type="submit">{loading ? 'Ingresando…' : 'Ingresar'}</button>
    </form>
    {error && <p role="alert" className="error">{error}</p>}
    <p className="hint">El JWT permanece solo en memoria: al recargar la página se pierde la sesión.</p></section>;
}
