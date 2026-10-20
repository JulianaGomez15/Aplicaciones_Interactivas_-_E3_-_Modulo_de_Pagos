import { Navigate, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import Login from './components/Login.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import InicioPage from './pages/InicioPage.jsx';
import NuevoPagoPage from './pages/NuevoPagoPage.jsx';
import PagosPage from './pages/PagosPage.jsx';
import ConsultaPagoPage from './pages/ConsultaPagoPage.jsx';
import MediosPagoPage from './pages/MediosPagoPage.jsx';
import NoAutorizadoPage from './pages/NoAutorizadoPage.jsx';
import { RUTAS_PRIVADAS } from './utils/rutas.js';

const PAGINAS = {
  '/servicio/pagos/nuevo': NuevoPagoPage,
  '/admin/pagos': PagosPage,
  '/pagos/consulta': ConsultaPagoPage,
  '/cliente/medios-pago': MediosPagoPage
};

export default function App() {
  return <><Navbar/><main className="container"><Routes>
    <Route path="/" element={<Navigate to="/inicio" replace/>}/>
    <Route path="/inicio" element={<InicioPage/>}/>
    <Route path="/login" element={<Login/>}/>
    {RUTAS_PRIVADAS.map(({ path, roles }) => {
      const Pagina = PAGINAS[path];
      return <Route key={path} path={path} element={<ProtectedRoute roles={roles}><Pagina/></ProtectedRoute>}/>;
    })}
    <Route path="/no-autorizado" element={<NoAutorizadoPage/>}/>
    <Route path="*" element={<Navigate to="/inicio" replace/>}/>
  </Routes></main></>;
}
