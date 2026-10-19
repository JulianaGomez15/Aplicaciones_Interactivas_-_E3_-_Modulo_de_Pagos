// La API no lista medios de pago (solo expone el alta, CU-08). Para poder usarlos despues desde "Iniciar pago",
// el front recuerda en este navegador los que se registraron. Solo guarda datos ya enmascarados.
const CLAVE = 'pagos.mediosRegistrados';

export function leerMedios() {
    try {
        return JSON.parse(localStorage.getItem(CLAVE) ?? '[]');
    } catch {
        return [];
    }
}

export function recordarMedio(medio, username) {
    try {
        const lista = [{...medio, registradoPor: username}, ...leerMedios()].slice(0, 30);
        localStorage.setItem(CLAVE, JSON.stringify(lista));
    } catch {
        // sin almacenamiento disponible: el codigo igual se puede copiar de la respuesta
    }
}
