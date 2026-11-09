// Pantalla "Generar reporte" (spec 11, pantalla 1).

import { api } from './api.js';
import { caja, cajaError, el, poner } from './dom.js';
import { fechaHora, MESES, mesAnterior } from './formato.js';
import { vistaDetalle } from './detalle.js';

export async function mostrarGenerar(contenedor) {
  const estadoPlantilla = el('div', {}, el('p', { class: 'cargando' }, 'Revisando la plantilla activa…'));
  const resultado = el('div', { 'aria-live': 'polite' });
  const { anio, mes } = mesAnterior();

  const campoArchivo = (id, etiqueta, ayuda, accept) => el('div', { class: 'campo' },
    el('label', { for: id }, etiqueta),
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
    el('div', { class: 'fila-campos' },
      campoArchivo('alestra', 'Alestra', 'Tráfico por línea (.xlsx)', '.xlsx'),
      campoArchivo('telcel', 'Telcel', 'Export de uso de datos (.csv o .xlsx)', '.csv,.xlsx'),
      campoArchivo('att', 'ATT', 'Consolidado de Jasper (.xlsx o .csv)', '.xlsx,.csv')),
    el('div', { class: 'acciones' }, boton));

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const archivos = ['alestra', 'telcel', 'att'].map((id) => formulario.elements[id].files[0]);
    const faltan = ['Alestra', 'Telcel', 'ATT'].filter((_, i) => !archivos[i]);
    if (faltan.length > 0) {
      poner(resultado, cajaError({ problemas: faltan.map((f) => `Falta el archivo de ${f}.`) }, 'Faltan archivos'));
      return;
    }
    boton.disabled = true;
    boton.textContent = 'Generando…';
    poner(resultado, caja('info', null, 'Generando el reporte; puede tardar unos segundos.'));
    try {
      const detalle = await api.generarReporte(formulario.elements.anio.value, formulario.elements.mes.value, ...archivos);
      poner(resultado,
        caja('ok', 'Reporte generado', `Se guardó como ${detalle.nombreArchivo}. También queda en el historial.`),
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
    el('p', { class: 'subtitulo' }, 'Sube los archivos de los tres operadores; la app llena la plantilla activa y verifica los totales.'),
    estadoPlantilla,
    el('section', { class: 'tarjeta' }, formulario),
    resultado);

  try {
    const activa = await api.plantillaActiva();
    poner(estadoPlantilla, activa
      ? caja('info', null, `Plantilla activa: versión ${activa.version.version} (${activa.mapeo.tablas.length} tablas) · subida ${fechaHora(activa.version.fechaCreacion)}`)
      : caja('alerta', 'No hay plantilla activa', 'Antes de generar un reporte, ', el('a', { href: '#plantilla' }, 'sube la plantilla'), '.'));
  } catch (e) {
    poner(estadoPlantilla, cajaError(e));
  }
}
