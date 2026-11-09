// Pantalla "Mi cuenta" (spec 12): datos propios y cambio de contraseña.
// Con contraseña temporal es la única pantalla disponible.

import { api } from './api.js';
import { caja, cajaError, el, poner } from './dom.js';

const ROLES = { SUPERADMIN: 'Superadmin', USUARIO: 'Usuario' };

export function mostrarCuenta(contenedor, sesion, alCambiar) {
  const mensajes = el('div', { 'aria-live': 'polite' });
  const campo = (id, etiqueta, autocomplete, ayuda) => el('div', { class: 'campo' },
    el('label', { for: id }, etiqueta),
    el('input', { type: 'password', id, name: id, autocomplete, required: true }),
    ayuda ? el('span', { class: 'ayuda' }, ayuda) : null);

  const boton = el('button', { type: 'submit', class: 'boton' }, 'Cambiar contraseña');
  const formulario = el('form', { class: 'formulario formulario-angosto', novalidate: true },
    campo('actual', sesion.debeCambiarPassword ? 'Contraseña temporal' : 'Contraseña actual', 'current-password'),
    campo('nueva', 'Contraseña nueva', 'new-password', 'Mínimo 10 caracteres. Puede ser una frase.'),
    campo('confirmar', 'Repite la contraseña nueva', 'new-password'),
    el('div', { class: 'acciones' }, boton));

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const { actual, nueva, confirmar } = formulario.elements;
    if (nueva.value !== confirmar.value) {
      poner(mensajes, cajaError({ problemas: ['La contraseña nueva y su repetición no coinciden.'] }, 'Revisa la contraseña'));
      return;
    }
    boton.disabled = true;
    try {
      const sesionNueva = await api.cambiarPassword(actual.value, nueva.value);
      formulario.reset();
      poner(mensajes, caja('ok', 'Contraseña cambiada', 'Tus otras sesiones abiertas se cerraron.'));
      alCambiar(sesionNueva);
    } catch (e) {
      poner(mensajes, cajaError(e, 'No se cambió la contraseña'));
    } finally {
      boton.disabled = false;
    }
  });

  poner(contenedor,
    el('h2', {}, 'Mi cuenta'),
    el('p', { class: 'subtitulo' }, `${sesion.nombreCompleto} · usuario ${sesion.usuario} · ${ROLES[sesion.rol] ?? sesion.rol}`),
    sesion.debeCambiarPassword
      ? caja('alerta', 'Cambia tu contraseña temporal', 'Antes de usar la app, elige una contraseña que solo tú conozcas.')
      : null,
    el('section', { class: 'tarjeta' }, el('h3', {}, 'Cambiar contraseña'), formulario),
    mensajes);
}
