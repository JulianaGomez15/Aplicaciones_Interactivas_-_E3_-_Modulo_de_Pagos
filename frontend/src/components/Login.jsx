import {useState} from 'react';
import {login} from '../services/pagosService.js';
import {CUENTAS} from '../data/demo.js';
import ErrorBanner from './ErrorBanner.jsx';

export default function Login({onSession}) {
    const [username, setUsername] = useState(CUENTAS[0].username);
    const [password, setPassword] = useState(CUENTAS[0].password);
    const [error, setError] = useState(null);

    function elegirCuenta(cuenta) {
        setUsername(cuenta.username);
        setPassword(cuenta.password);
    }

    async function entrar(e) {
        e.preventDefault();
        try {
            const data = await login(username, password);
            setError(null);
            onSession({token: data.token, username: data.username, roles: data.roles});
        } catch (err) {
            setError(err);
        }
    }

    return (
        <section className="card narrow">
            <h2>Ingresar</h2>
            <p className="muted">Cuentas de demo; cada rol ve y puede hacer solo lo que le corresponde.</p>
            <div className="chips">
                {CUENTAS.map((c) => (
                    <button type="button" key={c.username} title={c.descripcion}
                            className={c.username === username ? 'chip active' : 'chip'}
                            onClick={() => elegirCuenta(c)}>
                        {c.username}
                    </button>
                ))}
            </div>
            <p className="muted">{CUENTAS.find((c) => c.username === username)?.descripcion}</p>
            <form onSubmit={entrar} className="stack">
                <label>Usuario
                    <input value={username} onChange={(e) => setUsername(e.target.value)}/>
                </label>
                <label>Contraseña
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}/>
                </label>
                <button className="primary">Ingresar</button>
            </form>
            <ErrorBanner error={error}/>
        </section>
    );
}
