// Cliente de la API REST (spec 04, "Formatos"; spec 12).
// Rutas relativas: la página y la API viven en la misma app.
// Toda petición que cambia algo lleva el token CSRF (cookie XSRF-TOKEN → cabecera X-XSRF-TOKEN).

/** Error de la API con el formato RFC 9457 de la app: título y lista de problemas. */
export class ErrorApi extends Error {
  constructor(titulo, problemas, estado) {
    super(titulo);
    this.titulo = titulo;
    this.problemas = problemas;
    this.estado = estado;
  }
}

function tokenCsrf() {
  const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : null;
}

/**
 * @param sinSesion qué hacer con un 401: 'entrar' manda a la pantalla de entrada;
 *                  'error' lo deja como error (la propia pantalla de entrada).
 */
async function pedir(url, opciones = {}, sinSesion = 'entrar') {
  const metodo = (opciones.method ?? 'GET').toUpperCase();
  const cabeceras = new Headers(opciones.headers ?? {});
  if (metodo !== 'GET' && metodo !== 'HEAD') {
    const token = tokenCsrf();
    if (token) cabeceras.set('X-XSRF-TOKEN', token);
  }
  let respuesta;
  try {
    respuesta = await fetch(url, { ...opciones, headers: cabeceras, credentials: 'same-origin' });
  } catch {
    throw new ErrorApi('No se pudo conectar con la aplicación', ['Revisa tu conexión a internet.'], 0);
  }
  if (!respuesta.ok) {
    let cuerpo = null;
    try { cuerpo = await respuesta.json(); } catch { /* respuesta sin JSON */ }
    const problemas = Array.isArray(cuerpo?.problemas) && cuerpo.problemas.length > 0
      ? cuerpo.problemas
      : [cuerpo?.detail ?? `La aplicación respondió con el código ${respuesta.status}.`];
    const error = new ErrorApi(cuerpo?.title ?? 'Error', problemas, respuesta.status);
    if (respuesta.status === 401 && sinSesion === 'entrar') {
      location.replace('entrar.html');
    } else if (respuesta.status === 403 && error.titulo === 'Debes cambiar tu contraseña') {
      location.hash = '#cuenta';
    }
    throw error;
  }
  return respuesta;
}

const json = (r) => r.json();

export const api = {
  /** Deja la cookie XSRF-TOKEN; necesaria antes de entrar. */
  prepararEntrada() {
    return pedir('api/sesion/csrf', {}, 'error').then(json);
  },

  entrar(usuario, password) {
    const datos = new URLSearchParams({ usuario, password });
    return pedir('api/sesion/entrar', { method: 'POST', body: datos }, 'error').then(json);
  },

  /** Quién tiene la sesión; sin sesión manda a la pantalla de entrada. */
  sesion(sinSesion = 'entrar') {
    return pedir('api/sesion', {}, sinSesion).then(json);
  },

  salir() {
    return pedir('api/sesion/salir', { method: 'POST' }, 'error');
  },

  cambiarPassword(actual, nueva) {
    return pedir('api/sesion/password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ actual, nueva }),
    }).then(json);
  },

  /** Cómo lee la app la plantilla; no la guarda. */
  revisarPlantilla(plantilla) {
    const datos = new FormData();
    datos.append('plantilla', plantilla);
    return pedir('api/plantilla/revisar', { method: 'POST', body: datos }).then(json);
  },

  /** Genera el reporte; la respuesta trae el .xlsx en Base64 y no queda nada en el servidor. */
  generarReporte(anio, mes, { plantilla, alestra, telcel, att }) {
    const datos = new FormData();
    datos.append('anio', String(anio));
    datos.append('mes', String(mes));
    datos.append('plantilla', plantilla);
    datos.append('alestra', alestra);
    datos.append('telcel', telcel);
    datos.append('att', att);
    return pedir('api/reportes', { method: 'POST', body: datos }).then(json);
  },
};
