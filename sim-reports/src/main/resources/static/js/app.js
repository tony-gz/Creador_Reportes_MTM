// Navegación por #hash entre las pantallas (spec 11).

import { mostrarGenerar } from './vista-generar.js';
import { mostrarPlantilla } from './vista-plantilla.js';
import { mostrarHistorial, mostrarReporte } from './vista-historial.js';

const contenido = document.getElementById('contenido');

const VISTAS = {
  generar: () => mostrarGenerar(contenido),
  plantilla: () => mostrarPlantilla(contenido),
  historial: () => mostrarHistorial(contenido),
};

function navegar() {
  const ruta = location.hash.replace(/^#/, '') || 'generar';
  const [nombre, id] = ruta.split('/');
  const seccion = nombre === 'reporte' ? 'historial' : nombre;
  for (const enlace of document.querySelectorAll('.navegacion a')) {
    if (enlace.dataset.vista === seccion) enlace.setAttribute('aria-current', 'page');
    else enlace.removeAttribute('aria-current');
  }
  if (nombre === 'reporte' && id) {
    mostrarReporte(contenido, id);
  } else {
    (VISTAS[nombre] ?? VISTAS.generar)();
  }
  contenido.focus({ preventScroll: true });
  window.scrollTo(0, 0);
}

window.addEventListener('hashchange', navegar);
navegar();
