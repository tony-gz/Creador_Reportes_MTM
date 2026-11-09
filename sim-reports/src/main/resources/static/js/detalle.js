// Detalle de un reporte generado (spec 11, pantalla 4). Recibe la respuesta de
// POST /api/reportes o GET /api/reportes/{id}.

import { api } from './api.js';
import { el, caja, tabla } from './dom.js';
import { cantidad, fechaHora, fila, fuente, mb, periodo } from './formato.js';

export function vistaDetalle(d) {
  const avisos = d.avisos ?? {};
  return el('div', { class: 'detalle' },
    encabezado(d),
    seccionTotales(avisos.totales ?? [], avisos.advertenciasConsistencia ?? []),
    seccionRepetidas(avisos.simsRepetidas ?? []),
    seccionSinCliente(d.consumoSinCliente ?? []),
    seccionAvisos(avisos, avisos.simsRepetidas ?? []),
    seccionArchivos(d.archivosEntrada ?? []),
    seccionTexto(d.reporteFinal ?? ''));
}

function encabezado(d) {
  return el('section', { class: 'tarjeta' },
    el('div', { class: 'encabezado-detalle' },
      el('div', {},
        el('h2', {}, d.nombreArchivo),
        el('p', { class: 'subtitulo' },
          `Periodo ${periodo(d.periodoAnio, d.periodoMes)} · generado ${fechaHora(d.fecha)} · plantilla v${d.versionPlantilla}`)),
      el('a', { class: 'boton', href: api.urlDescarga(d.id), download: d.nombreArchivo }, 'Descargar .xlsx')));
}

function seccionTotales(totales, advertencias) {
  const indicadores = totales.map((t) => el('div', { class: 'indicador' },
    el('div', { class: 'nombre' }, fuente(t.fuente)),
    el('dl', {},
      el('dt', {}, 'En el archivo del operador'), el('dd', {}, mb(t.totalOrigen)),
      el('dt', {}, 'Asignado en la plantilla'), el('dd', {}, mb(t.totalAsignado)),
      el('dt', {}, 'Sin cliente'), el('dd', {}, mb(t.totalNoEncontrado)),
      t.totalRepetidoEntreHojas ? [el('dt', {}, 'Contado de nuevo en otra hoja'), el('dd', {}, mb(t.totalRepetidoEntreHojas))] : null)));
  const consistencia = advertencias.length === 0
    ? el('p', {}, el('span', { class: 'etiqueta-estado estado-ok' }, 'Los totales cuadran'))
    : caja('error', 'Los totales no cuadran', el('ul', {}, advertencias.map((a) => {
      const diferencia = a.totalOrigen - (a.totalAsignado - (a.totalRepetidoEntreHojas ?? 0) + a.totalNoEncontrado);
      return el('li', {}, `${fuente(a.fuente)}: diferencia de ${mb(diferencia)}`);
    })));
  return el('section', { class: 'tarjeta' },
    el('h3', {}, 'Totales por fuente'),
    consistencia,
    el('div', { class: 'indicadores' }, indicadores));
}

/** Agrupa las apariciones de una SIM por hoja, en orden. */
function porHoja(sim) {
  const grupos = new Map();
  for (const a of sim.apariciones ?? []) {
    if (!grupos.has(a.hoja)) grupos.set(a.hoja, []);
    grupos.get(a.hoja).push(a);
  }
  return grupos;
}

function seccionRepetidas(sims) {
  const mismaHoja = [];
  const entreHojas = [];
  for (const sim of sims) {
    const grupos = porHoja(sim);
    for (const [hoja, apariciones] of grupos) {
      if (apariciones.length < 2) continue;
      const conserva = apariciones.filter((a, i) => i === 0 || a.consumoAsignado !== 0).map(fila);
      const enCero = apariciones.filter((a, i) => i !== 0 && a.consumoAsignado === 0).map(fila);
      mismaHoja.push([hoja, `${fuente(sim.fuente)} ${sim.clave}`, mb(sim.consumoMb), conserva.join(', '), enCero.join(', ') || '—']);
    }
    if (grupos.size > 1) {
      const ubicaciones = [...grupos].map(([hoja, apariciones]) => `${hoja} ${fila(apariciones[0])}`);
      entreHojas.push([`${fuente(sim.fuente)} ${sim.clave}`, mb(sim.consumoMb), ubicaciones.join('; ')]);
    }
  }
  return el('section', { class: 'tarjeta' },
    el('h3', {}, 'SIMs repetidas'),
    el('h4', {}, `Dentro de una hoja (la primera conserva el consumo): ${mismaHoja.length}`),
    mismaHoja.length === 0 ? el('p', { class: 'vacio' }, 'Ninguna.') : tabla(
      [{ titulo: 'Hoja' }, { titulo: 'SIM', mono: true }, { titulo: 'Consumo', numero: true }, { titulo: 'Conserva el consumo' }, { titulo: 'En 0' }],
      mismaHoja),
    el('h4', {}, `En hojas distintas (se cuenta en cada hoja): ${entreHojas.length}`),
    entreHojas.length === 0 ? el('p', { class: 'vacio' }, 'Ninguna.') : tabla(
      [{ titulo: 'SIM', mono: true }, { titulo: 'Consumo en cada hoja', numero: true }, { titulo: 'Dónde aparece' }],
      entreHojas));
}

function seccionSinCliente(lista) {
  const porFuente = new Map();
  for (const n of lista) {
    if (!porFuente.has(n.fuente)) porFuente.set(n.fuente, []);
    porFuente.get(n.fuente).push(n);
  }
  const bloques = [...porFuente].map(([clave, items]) => {
    items.sort((a, b) => b.consumoMb - a.consumoMb || String(a.clave).localeCompare(String(b.clave)));
    const total = items.reduce((s, n) => s + n.consumoMb, 0);
    const tipo = clave === 'ALESTRA' ? 'líneas' : 'ICCID';
    return el('div', {},
      el('h4', {}, `${fuente(clave)}: ${cantidad(items.length)} ${tipo}, ${mb(total)}`),
      tabla([{ titulo: '#', numero: true }, { titulo: clave === 'ALESTRA' ? 'Línea' : 'ICCID', mono: true }, { titulo: 'Consumo', numero: true }],
        items.map((n, i) => [String(i + 1), n.clave, mb(n.consumoMb)]), { alta: items.length > 12 }));
  });
  return el('section', { class: 'tarjeta' },
    el('h3', {}, `Consumo sin cliente: ${cantidad(lista.length)}`),
    el('p', { class: 'subtitulo' }, 'Líneas e ICCID con consumo en los archivos de los operadores que no están en ninguna hoja de la plantilla.'),
    lista.length === 0 ? el('p', { class: 'vacio' }, 'Ninguno.') : bloques);
}

function seccionAvisos(avisos, sims) {
  const repetidas = new Set(sims.map((s) => s.clave));
  const lista = (avisos.avisosParseo ?? [])
    .filter((a) => !(a.tipo === 'CLAVE_DUPLICADA' && repetidas.has(a.clave)));
  const formulas = avisos.formulasSinRecalcular ?? 0;
  return el('section', { class: 'tarjeta' },
    el('h3', {}, `Otros avisos: ${cantidad(lista.length)}`),
    formulas > 0 ? caja('alerta', null, `${formulas} fórmula(s) no se pudieron recalcular; Excel las recalcula al abrir el archivo.`) : null,
    lista.length === 0 ? el('p', { class: 'vacio' }, 'Ninguno.')
      : el('ul', { class: 'lista-simple' }, lista.map((a) => el('li', {},
        el('span', { class: 'chip' }, a.tipo), ' ', el('span', { class: 'mono' }, a.clave), ': ', a.detalle))));
}

function seccionArchivos(archivos) {
  return el('section', { class: 'tarjeta' },
    el('h3', {}, 'Archivos usados'),
    tabla([{ titulo: 'Fuente' }, { titulo: 'Archivo' }, { titulo: 'Tamaño', numero: true }],
      archivos.map((a) => [fuente(a.fuente), a.nombre, `${cantidad(Math.ceil(a.bytes / 1024))} KB`])));
}

function seccionTexto(texto) {
  const estado = el('span', { class: 'suave', 'aria-live': 'polite' });
  const copiar = el('button', {
    type: 'button',
    class: 'boton boton-secundario boton-chico',
    onclick: async () => {
      try {
        await navigator.clipboard.writeText(texto);
        estado.textContent = 'Copiado.';
      } catch {
        estado.textContent = 'No se pudo copiar; selecciona el texto a mano.';
      }
    },
  }, 'Copiar texto');
  return el('section', { class: 'tarjeta' },
    el('details', {},
      el('summary', {}, 'Texto completo del reporte final'),
      el('div', { class: 'acciones', style: 'margin: 12px 0' }, copiar, estado),
      el('pre', { class: 'reporte' }, texto)));
}
