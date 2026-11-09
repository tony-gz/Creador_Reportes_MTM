// Pantalla de entrada (spec 12, D-026).

import { api } from './api.js';
import { cajaError, poner } from './dom.js';

const formulario = document.getElementById('formulario-entrar');
const mensaje = document.getElementById('mensaje-entrar');
const boton = document.getElementById('boton-entrar');

function irALaApp(sesion) {
  location.replace(sesion.debeCambiarPassword ? './#cuenta' : './');
}

async function iniciar() {
  try {
    irALaApp(await api.sesion('error')); // ya tenía sesión
    return;
  } catch {
    // sin sesión: se queda aquí
  }
  try {
    await api.prepararEntrada();
  } catch (e) {
    poner(mensaje, cajaError(e));
  }
  formulario.elements.usuario.focus();
}

formulario.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const usuario = formulario.elements.usuario.value.trim();
  const password = formulario.elements.password.value;
  if (!usuario || !password) {
    poner(mensaje, cajaError({ problemas: ['Escribe tu usuario y tu contraseña.'] }, 'Faltan datos'));
    return;
  }
  boton.disabled = true;
  boton.textContent = 'Entrando…';
  try {
    irALaApp(await api.entrar(usuario, password));
  } catch (e) {
    formulario.elements.password.value = '';
    poner(mensaje, cajaError(e));
    if (e.estado === 403) await api.prepararEntrada().catch(() => {}); // token vencido: pide otro
  } finally {
    boton.disabled = false;
    boton.textContent = 'Entrar';
  }
});

iniciar();
