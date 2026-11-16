// Genera el cuerpo del documento (sin caratula ni encabezados) a partir de los fragmentos HTML.
// Uso: node generar.js <ruta a mermaid.min.js> <salida.pdf>
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const [, , mermaidJs, salida] = process.argv;
const dir = __dirname;
const fragmentos = fs.readdirSync(dir).filter(f => /^\d\d-.*\.html$/.test(f)).sort();
const cuerpo = fragmentos.map(f => fs.readFileSync(path.join(dir, f), 'utf8')).join('\n');
const html = `<!doctype html><html lang="es"><head><meta charset="utf-8">
<style>${fs.readFileSync(path.join(dir, 'estilo.css'), 'utf8')}</style></head><body>${cuerpo}</body></html>`;

// Alto maximo de cada tipo de figura, en cm (el ancho maximo sale de la pagina).
const ALTOS = { ancha: 6.2, sec1: 10.5, sec: 13.2, media: 16, alta: 20.2, apaisada: 12.4 };

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.setContent(html, { waitUntil: 'load' });
  await page.addScriptTag({ path: mermaidJs });
  await page.evaluate(async (ALTOS) => {
    const fuente = '"Liberation Sans", Arial, sans-serif';
    mermaid.initialize({
      startOnLoad: false, theme: 'default', securityLevel: 'loose', fontFamily: fuente,
      themeVariables: { fontFamily: fuente, fontSize: '14px' },
      sequence: { useMaxWidth: false, wrap: false, mirrorActors: false, messageMargin: 12, boxMargin: 6,
                  actorMargin: 30, noteMargin: 8, height: 40, width: 128, messageFontSize: 16, actorFontSize: 16,
                  noteFontSize: 15 },
      flowchart: { useMaxWidth: false, htmlLabels: true, wrappingWidth: 460, curve: 'basis', nodeSpacing: 45, rankSpacing: 62,
                   padding: 12 },
      class: { useMaxWidth: false }, er: { useMaxWidth: false }, state: { useMaxWidth: false },
    });
    await document.fonts.ready;
    await mermaid.run({ querySelector: 'pre.mermaid' });
    const CM = 37.795;
    for (const figura of document.querySelectorAll('figure')) {
      const svg = figura.querySelector('svg');
      if (!svg) continue;
      const apaisada = figura.closest('section.apaisada') !== null;
      const anchoMax = (apaisada ? 25.9 : 17.2) * CM;
      const clase = [...figura.classList].find(c => ALTOS[c]);
      const propio = figura.style.getPropertyValue('--h');
      const altoMax = (propio ? parseFloat(propio) : ALTOS[clase]) * CM;
      const [, , w, h] = svg.getAttribute('viewBox').split(/\s+/).map(Number);
      const escala = Math.min(anchoMax / w, altoMax / h, 1.15);
      svg.removeAttribute('style');
      svg.setAttribute('width', Math.floor(w * escala));
      svg.setAttribute('height', Math.floor(h * escala));
      figura.dataset.escala = escala.toFixed(2) + ' (' + Math.round(w) + 'x' + Math.round(h) + ')';
    }
  }, ALTOS);
  console.log(await page.evaluate(() => [...document.querySelectorAll('figure')].map(f => f.dataset.escala).join(' | ')));
  await page.pdf({ path: salida, preferCSSPageSize: true, printBackground: true });
  await browser.close();
})();
