# Fuentes del documento del TPO

De acá sale `src/main/java/ar/edu/uade/api/pagos/PDF/TPO - Grupo 3 - Módulo de Pagos actualizado.pdf`.

- `01-…` a `05-….html`: el contenido, en orden. Los diagramas están escritos en Mermaid dentro de cada archivo
  (`<pre class="mermaid">`), así que se editan como texto.
- `estilo.css`: formato de páginas, títulos y tablas.
- `generar.js`: arma el cuerpo en PDF con Chromium (Playwright) y Mermaid.
- `componer.py`: le agrega la carátula (la primera página del PDF de la Entrega 1), el encabezado y el pie con el
  número de página.

## Regenerar

Hace falta Node con `playwright` y `mermaid` (probado con 11.4.1), Python con `pypdf` y `reportlab`, y las fuentes
Carlito y Liberation Sans.

```bash
npm install mermaid@11.4.1 playwright
```

```bash
node docs/documento/generar.js node_modules/mermaid/dist/mermaid.min.js cuerpo.pdf
```

```bash
python3 docs/documento/componer.py "src/main/java/ar/edu/uade/api/pagos/PDF/TPO - Entrega 1 - Grupo 3 - Módulo de Pagos corregido.pdf" cuerpo.pdf "src/main/java/ar/edu/uade/api/pagos/PDF/TPO - Grupo 3 - Módulo de Pagos actualizado.pdf"
```
