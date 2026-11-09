// Navegación por #hash entre las pantallas (spec 11 y 12). Sin sesión, api.js
// manda a la pantalla de entrada; con contraseña temporal solo queda "Mi cuenta".

import { api } from './api.js';
import { el, poner } from './dom.js';
import { mostrarGenerar } from './vista-generar.js';
import { mostrarPlantilla } from './vista-plantilla.js';
import { mostrarCuenta } from './vista-cuenta.js';

const contenido = document.getElementById('contenido');
const navegacion = document.getElementById('navegacion');
const barraUsuario = document.getElementById('usuario-barra');

let sesion = await api.sesion();

const VISTAS = {
  generar: () => mostrarGenerar(contenido),
  plantilla: () => mostrarPlantilla(contenido),
  cuenta: () => mostrarCuenta(contenido, sesion, (nueva) => {
    const eraTemporal = sesion.debeCambiarPassword;
    sesion = nueva;
    pintarBarra();
    if (eraTemporal) location.hash = '#generar';
  }),
};

function pintarBarra() {
  navegacion.hidden = sesion.debeCambiarPassword;
  const salir = el('button', { type: 'button', class: 'boton boton-secundario boton-chico' }, 'Salir');
  salir.addEventListener('click', async () => {
    salir.disabled = true;
    try { await api.salir(); } catch { /* aunque falle, se va a la entrada */ }
    location.replace('entrar.html');
  });
  poner(barraUsuario,
    el('span', { class: 'quien' }, sesion.nombreCompleto),
    el('a', { href: '#cuenta', class: 'boton boton-secundario boton-chico', dataset: { vista: 'cuenta' } }, 'Mi cuenta'),
    salir);
}

function navegar() {
  let nombre = location.hash.replace(/^#/, '').split('/')[0] || 'generar';
  if (sesion.debeCambiarPassword) nombre = 'cuenta';
  const seccion = VISTAS[nombre] ? nombre : 'generar';
  for (const enlace of document.querySelectorAll('[data-vista]')) {
    if (enlace.dataset.vista === seccion) enlace.setAttribute('aria-current', 'page');
    else enlace.removeAttribute('aria-current');
  }
  VISTAS[seccion]();
  contenido.focus({ preventScroll: true });
  window.scrollTo(0, 0);
}

pintarBarra();
window.addEventListener('hashchange', navegar);
navegar();
