import test from 'node:test';
import assert from 'node:assert/strict';
import { rutaInicial, rutasPermitidas } from '../src/utils/rutas.js';

test('cada rol ve solo las pantallas de la tabla de autorización', () => {
  assert.deepEqual(rutasPermitidas(['SERVICIO_ESCRITURA']).map(r => r.path), ['/servicio/pagos/nuevo']);
  assert.deepEqual(rutasPermitidas(['ANALISTA']).map(r => r.path), ['/admin/pagos']);
  assert.deepEqual(rutasPermitidas(['CLIENTE']).map(r => r.path), ['/pagos/consulta', '/cliente/medios-pago']);
  assert.deepEqual(rutasPermitidas(['ADMINISTRADOR']).map(r => r.path), ['/admin/pagos', '/pagos/consulta']);
});

test('la ruta inicial es la primera permitida, o el inicio público', () => {
  assert.equal(rutaInicial(['SERVICIO_ESCRITURA', 'SERVICIO_LECTURA']), '/servicio/pagos/nuevo');
  assert.equal(rutaInicial(['SERVICIO_LECTURA']), '/admin/pagos');
  assert.equal(rutaInicial([]), '/inicio');
});
