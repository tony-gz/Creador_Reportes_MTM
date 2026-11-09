// Navegación por #hash entre las pantallas (spec 11 y 12).

import { mostrarGenerar } from './vista-generar.js';
import { mostrarPlantilla } from './vista-plantilla.js';

const contenido = document.getElementById('contenido');

const VISTAS = {
  generar: () => mostrarGenerar(contenido),
  plantilla: () => mostrarPlantilla(contenido),
};

function navegar() {
  const nombre = location.hash.replace(/^#/, '').split('/')[0] || 'generar';
  const seccion = VISTAS[nombre] ? nombre : 'generar';
  for (const enlace of document.querySelectorAll('.navegacion a')) {
    if (enlace.dataset.vista === seccion) enlace.setAttribute('aria-current', 'page');
    else enlace.removeAttribute('aria-current');
  }
  VISTAS[seccion]();
  contenido.focus({ preventScroll: true });
  window.scrollTo(0, 0);
}

window.addEventListener('hashchange', navegar);
navegar();
