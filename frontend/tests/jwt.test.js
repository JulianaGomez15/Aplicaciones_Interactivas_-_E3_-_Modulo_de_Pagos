import test from 'node:test';
import assert from 'node:assert/strict';
import { leerClaims, rolesDelToken, tieneAlgunRol, tokenVigente } from '../src/utils/jwt.js';

const makeToken = obj => `header.${Buffer.from(JSON.stringify(obj)).toString('base64url')}.signature`;

test('decodifica roles sin confundirlos con la firma', () => {
  const claims = leerClaims(makeToken({ sub: 'admin', roles: ['ADMINISTRADOR'], exp: 4102444800 }));
  assert.deepEqual(rolesDelToken(claims), ['ADMINISTRADOR']);
  assert.equal(tokenVigente(claims), true);
});

test('rechaza token vencido o mal formado para navegación', () => {
  assert.equal(tokenVigente(leerClaims(makeToken({ exp: 1 }))), false);
  assert.equal(leerClaims('invalido'), null);
});

test('soporta arreglo de roles con prefijo ROLE_ y el claim role simple', () => {
  assert.deepEqual(rolesDelToken({ roles: ['ROLE_SERVICIO_ESCRITURA', 'ROLE_SERVICIO_LECTURA'] }),
    ['SERVICIO_ESCRITURA', 'SERVICIO_LECTURA']);
  assert.deepEqual(rolesDelToken({ role: 'ADMIN' }), ['ADMIN']);
});

test('tieneAlgunRol acepta si coincide al menos un rol', () => {
  assert.equal(tieneAlgunRol(['CLIENTE'], ['CLIENTE', 'ADMINISTRADOR']), true);
  assert.equal(tieneAlgunRol(['ANALISTA'], ['CLIENTE']), false);
  assert.equal(tieneAlgunRol([], []), true);
});
