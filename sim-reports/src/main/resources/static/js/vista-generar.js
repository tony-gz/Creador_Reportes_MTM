// Pantalla "Generar reporte" (spec 11, pantalla 1; D-023 en spec 12):
// se suben la plantilla y los 3 archivos; nada queda guardado en el servidor.

import { api } from './api.js';
import { caja, cajaError, el, poner } from './dom.js';
import { MESES, mesAnterior } from './formato.js';
import { vistaDetalle } from './detalle.js';

const ARCHIVOS = [
  { id: 'plantilla', nombre: 'Plantilla', ayuda: 'El libro de clientes, p. ej. el reporte del mes anterior (.xlsx)', accept: '.xlsx' },
  { id: 'alestra', nombre: 'Alestra', ayuda: 'Tráfico por línea (.xlsx)', accept: '.xlsx' },
  { id: 'telcel', nombre: 'Telcel', ayuda: 'Export de uso de datos (.csv o .xlsx)', accept: '.csv,.xlsx' },
  { id: 'att', nombre: 'ATT', ayuda: 'Consolidado de Jasper (.xlsx o .csv)', accept: '.xlsx,.csv' },
];

export function mostrarGenerar(contenedor) {
  const resultado = el('div', { 'aria-live': 'polite' });
  const { anio, mes } = mesAnterior();

  const campoArchivo = ({ id, nombre, ayuda, accept }) => el('div', { class: 'campo' },
    el('label', { for: id }, nombre),
    el('input', { type: 'file', id, name: id, accept, required: true }),
    el('span', { class: 'ayuda' }, ayuda));

  const boton = el('button', { type: 'submit', class: 'boton' }, 'Generar reporte');
  const formulario = el('form', { class: 'formulario', novalidate: true },
    el('div', { class: 'fila-campos' },
      el('div', { class: 'campo' },
        el('label', { for: 'mes' }, 'Mes'),
        el('select', { id: 'mes', name: 'mes' },
          MESES.map((nombre, i) => el('option', { value: i + 1, selected: i + 1 === mes }, nombre)))),
      el('div', { class: 'campo' },
        el('label', { for: 'anio' }, 'Año'),
        el('input', { type: 'number', id: 'anio', name: 'anio', min: 2000, max: 2100, value: anio, required: true }))),
    el('div', { class: 'fila-campos' }, ARCHIVOS.slice(0, 2).map(campoArchivo)),
    el('div', { class: 'fila-campos' }, ARCHIVOS.slice(2).map(campoArchivo)),
    el('div', { class: 'acciones' }, boton));

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const archivos = Object.fromEntries(ARCHIVOS.map(({ id }) => [id, formulario.elements[id].files[0]]));
    const faltan = ARCHIVOS.filter(({ id }) => !archivos[id]).map(({ nombre }) => nombre);
    if (faltan.length > 0) {
      poner(resultado, cajaError({ problemas: faltan.map((f) => `Falta el archivo: ${f}.`) }, 'Faltan archivos'));
      return;
    }
    boton.disabled = true;
    boton.textContent = 'Generando…';
    poner(resultado, caja('info', null, 'Generando el reporte; puede tardar unos segundos.'));
    try {
      const detalle = await api.generarReporte(formulario.elements.anio.value, formulario.elements.mes.value, archivos);
      poner(resultado,
        caja('ok', 'Reporte generado', `Descarga ${detalle.nombreArchivo} y guárdalo: la app no se queda con ninguna copia.`),
        vistaDetalle(detalle));
      resultado.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (e) {
      poner(resultado, cajaError(e, 'No se generó el reporte'));
    } finally {
      boton.disabled = false;
      boton.textContent = 'Generar reporte';
    }
  });

  poner(contenedor,
    el('h2', {}, 'Generar reporte'),
    el('p', { class: 'subtitulo' }, 'Sube la plantilla y los archivos de los tres operadores. La app llena la plantilla, verifica los totales y te devuelve el Excel. No guarda ningún archivo.'),
    el('section', { class: 'tarjeta' }, formulario),
    resultado);
}
