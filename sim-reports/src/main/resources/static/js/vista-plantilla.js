// Pantalla "Revisar plantilla" (spec 11, pantalla 2; D-023 en spec 12):
// muestra cómo lee la app la plantilla, sin guardarla.

import { api } from './api.js';
import { caja, cajaError, el, poner } from './dom.js';
import { tipoTabla } from './formato.js';

const CAMPOS = ['SIM', 'LINEA', 'ICCID', 'LINEA_1', 'LINEA_2', 'CONSUMO(MB)', 'CONSUMO(MB)_1', 'CONSUMO(MB)_2'];

export function mostrarPlantilla(contenedor) {
  const resultado = el('div', { 'aria-live': 'polite' });
  const entrada = el('input', { type: 'file', id: 'plantilla', accept: '.xlsx', required: true });
  const boton = el('button', { type: 'submit', class: 'boton' }, 'Revisar');
  const formulario = el('form', { class: 'formulario' },
    el('div', { class: 'campo' },
      el('label', { for: 'plantilla' }, 'Archivo de la plantilla (.xlsx)'),
      entrada,
      el('span', { class: 'ayuda' }, 'No se guarda. Si tiene problemas verás la lista completa.')),
    el('div', { class: 'acciones' }, boton));

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const archivo = entrada.files[0];
    if (!archivo) {
      poner(resultado, cajaError({ problemas: ['Elige el archivo de la plantilla.'] }, 'Falta el archivo'));
      return;
    }
    boton.disabled = true;
    boton.textContent = 'Revisando…';
    try {
      const mapeo = await api.revisarPlantilla(archivo);
      poner(resultado,
        caja('ok', 'La plantilla se puede usar', `${mapeo.tablas.length} tablas reconocidas en ${archivo.name}.`),
        vistaMapeo(mapeo));
    } catch (e) {
      poner(resultado, cajaError(e, 'La plantilla tiene problemas'));
    } finally {
      boton.disabled = false;
      boton.textContent = 'Revisar';
    }
  });

  poner(contenedor,
    el('h2', {}, 'Revisar plantilla'),
    el('p', { class: 'subtitulo' }, 'Antes de generar el reporte puedes ver cómo interpreta la app cada hoja. Cada hoja de clientes debe tener sus SIMs en tablas de Excel (Insertar → Tabla); si una columna queda mal, corrígela en el Excel.'),
    el('section', { class: 'tarjeta' }, formulario),
    resultado);
}

function vistaMapeo(mapeo) {
  return el('section', { class: 'tarjeta' },
    el('h3', {}, 'Cómo la lee la app'),
    el('div', { class: 'tabla-contenedor' },
      el('table', {},
        el('thead', {}, el('tr', {}, ['Hoja', 'Tabla', 'Búsqueda', 'Columnas reconocidas', 'Otras columnas'].map((h) => el('th', { scope: 'col' }, h)))),
        el('tbody', {}, mapeo.tablas.map((t) => el('tr', {},
          el('td', {}, t.hoja),
          el('td', {}, t.nombreTabla, el('div', { class: 'suave' }, t.rango)),
          el('td', {}, tipoTabla(t.tipo)),
          el('td', {}, el('div', { class: 'chips' }, CAMPOS.filter((c) => t.columnaPorCampoLogico[c])
            .map((c) => el('span', { class: 'chip' }, c === t.columnaPorCampoLogico[c] ? c : `${c} ← ${t.columnaPorCampoLogico[c]}`)))),
          el('td', {}, t.encabezadosNoReconocidos.length === 0 ? el('span', { class: 'suave' }, '—')
            : el('div', { class: 'chips' }, t.encabezadosNoReconocidos.map((h) => el('span', { class: 'chip' }, h))))))))));
}
