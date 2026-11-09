// Pantalla "Historial" (spec 11, pantalla 3) y detalle de una corrida (pantalla 4).

import { api } from './api.js';
import { cajaError, el, poner, tabla } from './dom.js';
import { fechaHora, periodo } from './formato.js';
import { vistaDetalle } from './detalle.js';

export async function mostrarHistorial(contenedor) {
  poner(contenedor, el('h2', {}, 'Historial'), el('p', { class: 'cargando' }, 'Cargando…'));
  try {
    const lista = await api.reportes();
    poner(contenedor,
      el('h2', {}, 'Historial'),
      el('p', { class: 'subtitulo' }, 'Reportes generados, el más reciente primero.'),
      el('section', { class: 'tarjeta' }, lista.length === 0
        ? el('p', { class: 'vacio' }, 'Todavía no se ha generado ningún reporte.')
        : tabla([{ titulo: 'Periodo' }, { titulo: 'Archivo' }, { titulo: 'Generado' }, { titulo: 'Plantilla' }, { titulo: '' }],
          lista.map((r) => [
            periodo(r.periodoAnio, r.periodoMes),
            r.nombreArchivo,
            fechaHora(r.fecha),
            `v${r.versionPlantilla}`,
            el('div', { class: 'acciones' },
              el('a', { class: 'boton boton-secundario boton-chico', href: `#reporte/${r.id}` }, 'Ver detalle'),
              el('a', { class: 'boton boton-chico', href: api.urlDescarga(r.id), download: r.nombreArchivo }, 'Descargar')),
          ]))));
  } catch (e) {
    poner(contenedor, el('h2', {}, 'Historial'), cajaError(e));
  }
}

export async function mostrarReporte(contenedor, id) {
  poner(contenedor, el('p', { class: 'cargando' }, 'Cargando el reporte…'));
  try {
    const detalle = await api.reporte(id);
    poner(contenedor, el('p', {}, el('a', { href: '#historial' }, '← Historial')), vistaDetalle(detalle));
  } catch (e) {
    poner(contenedor, el('p', {}, el('a', { href: '#historial' }, '← Historial')), cajaError(e));
  }
}
