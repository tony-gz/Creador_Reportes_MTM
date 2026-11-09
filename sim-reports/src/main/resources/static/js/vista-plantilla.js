// Pantalla "Plantilla" (spec 11, pantalla 2): versión activa, subir una nueva,
// mapeo por tabla con corrección manual, e historial de versiones.

import { api } from './api.js';
import { caja, cajaError, el, poner, tabla } from './dom.js';
import { fechaHora, tipoTabla } from './formato.js';

const CAMPOS = ['SIM', 'LINEA', 'ICCID', 'LINEA_1', 'LINEA_2', 'CONSUMO(MB)', 'CONSUMO(MB)_1', 'CONSUMO(MB)_2'];

export async function mostrarPlantilla(contenedor) {
  const mensajes = el('div', { 'aria-live': 'polite' });
  const activa = el('div', {}, el('p', { class: 'cargando' }, 'Cargando…'));
  const versiones = el('div', {});

  const entrada = el('input', { type: 'file', id: 'plantilla', accept: '.xlsx', required: true });
  const boton = el('button', { type: 'submit', class: 'boton' }, 'Subir plantilla');
  const formulario = el('form', { class: 'formulario' },
    el('div', { class: 'campo' },
      el('label', { for: 'plantilla' }, 'Archivo de la plantilla (.xlsx)'),
      entrada,
      el('span', { class: 'ayuda' }, 'Queda como versión activa. Si tiene problemas no se guarda y verás la lista completa.')),
    el('div', { class: 'acciones' }, boton));

  async function recargar() {
    try {
      const [plantilla, lista] = await Promise.all([api.plantillaActiva(), api.versionesPlantilla()]);
      poner(activa, plantilla ? vistaMapeo(plantilla, mensajes, recargar) : caja('alerta', 'Todavía no hay plantilla', 'Sube la plantilla para poder generar reportes.'));
      poner(versiones, lista.length === 0 ? el('p', { class: 'vacio' }, 'Sin versiones.') : tabla(
        [{ titulo: 'Versión', numero: true }, { titulo: 'Subida' }, { titulo: 'Estado' }],
        lista.map((v) => [`v${v.version}`, fechaHora(v.fechaCreacion),
          v.activa ? el('span', { class: 'etiqueta-estado estado-ok' }, 'Activa') : el('span', { class: 'suave' }, 'Anterior')])));
    } catch (e) {
      poner(activa, cajaError(e));
    }
  }

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const archivo = entrada.files[0];
    if (!archivo) {
      poner(mensajes, cajaError({ problemas: ['Elige el archivo de la plantilla.'] }, 'Falta el archivo'));
      return;
    }
    boton.disabled = true;
    boton.textContent = 'Subiendo…';
    try {
      const r = await api.subirPlantilla(archivo);
      poner(mensajes, caja('ok', `Plantilla guardada como versión ${r.version.version}`, `${r.mapeo.tablas.length} tablas reconocidas.`));
      formulario.reset();
      await recargar();
    } catch (e) {
      poner(mensajes, cajaError(e, 'La plantilla no se guardó'));
    } finally {
      boton.disabled = false;
      boton.textContent = 'Subir plantilla';
    }
  });

  poner(contenedor,
    el('h2', {}, 'Plantilla'),
    el('p', { class: 'subtitulo' }, 'El libro de Excel con las hojas de clientes. Cada hoja debe tener sus SIMs en tablas de Excel (Insertar → Tabla).'),
    el('section', { class: 'tarjeta' }, el('h3', {}, 'Subir una plantilla nueva'), formulario),
    mensajes,
    activa,
    el('section', { class: 'tarjeta' }, el('h3', {}, 'Versiones'), versiones));
  await recargar();
}

function vistaMapeo(plantilla, mensajes, recargar) {
  const { version, mapeo } = plantilla;
  const cuerpo = el('tbody', {});
  for (const t of mapeo.tablas) {
    const filaTabla = el('tr', {},
      el('td', {}, t.hoja),
      el('td', {}, t.nombreTabla, el('div', { class: 'suave' }, t.rango)),
      el('td', {}, tipoTabla(t.tipo)),
      el('td', {}, el('div', { class: 'chips' }, CAMPOS.filter((c) => t.columnaPorCampoLogico[c])
        .map((c) => el('span', { class: 'chip' }, c === t.columnaPorCampoLogico[c] ? c : `${c} ← ${t.columnaPorCampoLogico[c]}`)))),
      el('td', {}, t.encabezadosNoReconocidos.length === 0 ? el('span', { class: 'suave' }, '—')
        : el('div', { class: 'chips' }, t.encabezadosNoReconocidos.map((h) => el('span', { class: 'chip' }, h)))),
      el('td', {}));
    const editar = el('button', { type: 'button', class: 'boton boton-secundario boton-chico' }, 'Corregir');
    filaTabla.lastChild.append(editar);
    editar.addEventListener('click', () => {
      const abierto = filaTabla.nextSibling?.classList?.contains('editor-mapeo');
      if (abierto) {
        filaTabla.nextSibling.remove();
        return;
      }
      filaTabla.after(editorMapeo(t, mensajes, recargar));
    });
    cuerpo.append(filaTabla);
  }
  return el('section', { class: 'tarjeta' },
    el('h3', {}, `Versión activa: v${version.version}`),
    el('p', { class: 'subtitulo' }, `Subida ${fechaHora(version.fechaCreacion)} · ${mapeo.tablas.length} tablas. Si una columna quedó mal interpretada, corrígela aquí: no se crea una versión nueva.`),
    el('div', { class: 'tabla-contenedor' },
      el('table', {},
        el('thead', {}, el('tr', {}, ['Hoja', 'Tabla', 'Búsqueda', 'Columnas reconocidas', 'Otras columnas', ''].map((h) => el('th', { scope: 'col' }, h)))),
        cuerpo)));
}

/** Fila con un selector por campo lógico; las opciones son los encabezados reales de la tabla. */
function editorMapeo(t, mensajes, recargar) {
  const encabezados = [...new Set([...Object.values(t.columnaPorCampoLogico), ...t.encabezadosNoReconocidos])];
  const selectores = CAMPOS.map((campo) => {
    const id = `map-${t.hoja}-${t.nombreTabla}-${campo}`.replace(/[^A-Za-z0-9_-]/g, '_');
    const select = el('select', { id, dataset: { campo } },
      el('option', { value: '' }, '— sin asignar —'),
      encabezados.map((h) => el('option', { value: h, selected: t.columnaPorCampoLogico[campo] === h }, h)));
    return el('div', { class: 'campo' }, el('label', { for: id }, campo), select);
  });
  const estado = el('div', {});
  const guardar = el('button', { type: 'button', class: 'boton boton-chico' }, 'Guardar corrección');
  guardar.addEventListener('click', async () => {
    const columnas = {};
    for (const s of selectores.map((d) => d.querySelector('select'))) {
      if (s.value) columnas[s.dataset.campo] = s.value;
    }
    guardar.disabled = true;
    try {
      await api.corregirMapeo(t.hoja, t.nombreTabla, columnas);
      poner(mensajes, caja('ok', 'Mapeo corregido', `${t.hoja.trim()} · ${t.nombreTabla} se guardó en la versión activa.`));
      await recargar();
    } catch (e) {
      poner(estado, cajaError(e, 'No se guardó la corrección'));
      guardar.disabled = false;
    }
  });
  return el('tr', { class: 'editor-mapeo' },
    el('td', { colspan: 6 },
      el('p', { class: 'ayuda' }, 'Elige qué columna de la tabla corresponde a cada campo. La app vuelve a clasificar la tabla y revisa que toda la plantilla se pueda leer.'),
      el('div', { class: 'fila-campos' }, selectores),
      estado,
      el('div', { class: 'acciones', style: 'margin-top: 12px' }, guardar)));
}
