import {useState} from 'react';
import Login from './components/Login.jsx';
import NuevoPagoPage from './pages/NuevoPagoPage.jsx';
import PagosPage from './pages/PagosPage.jsx';
import ConsultaPage from './pages/ConsultaPage.jsx';
import MediosPagoPage from './pages/MediosPagoPage.jsx';

// Pestañas visibles por rol (el backend es quien hace cumplir los permisos).
const TABS = [
    {id: 'nuevo', titulo: 'Iniciar pago', roles: ['ADMIN', 'PEDIDOS'], Page: NuevoPagoPage},
    {id: 'pagos', titulo: 'Pagos', roles: ['ADMIN', 'ANALISTA'], Page: PagosPage},
    {id: 'consulta', titulo: 'Consultar', roles: ['ADMIN', 'PEDIDOS', 'ANALISTA', 'CLIENTE'], Page: ConsultaPage},
    {id: 'medios', titulo: 'Medios de pago', roles: ['ADMIN', 'CLIENTE'], Page: MediosPagoPage}
];

export default function App() {
    const [session, setSession] = useState(null);
    const [tabId, setTabId] = useState(null);

    function entrar(s) {
        setSession(s);
        setTabId(TABS.find((t) => t.roles.includes(s.rol)).id);
    }

    function salir() {
        setSession(null);
        setTabId(null);
    }

    const tabs = session ? TABS.filter((t) => t.roles.includes(session.rol)) : [];
    const activa = tabs.find((t) => t.id === tabId);

    return (
        <main>
            <header>
                <div>
                    <h1>DeliveryGo · Módulo de Pagos</h1>
                    <p className="muted">Pagos simulados: no se procesa dinero real. Grupo 3.</p>
                </div>
                {session && (
                    <div className="session">
                        <span>{session.username} <span className="badge role">{session.rol}</span></span>
                        <button className="chip" onClick={salir}>Salir</button>
                    </div>
                )}
            </header>

            {!session && <Login onSession={entrar}/>}

            {session && (
                <>
                    <nav>
                        {tabs.map((t) => (
                            <button key={t.id} className={t.id === tabId ? 'tab active' : 'tab'}
                                    onClick={() => setTabId(t.id)}>{t.titulo}</button>
                        ))}
                    </nav>
                    {activa && <activa.Page key={activa.id} session={session}/>}
                </>
            )}
        </main>
    );
}
