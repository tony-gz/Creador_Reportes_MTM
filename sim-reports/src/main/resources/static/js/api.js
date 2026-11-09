// Cliente de la API REST de la Fase 2 (spec 04, "Formatos").
// Rutas relativas: la página y la API viven en la misma app.

/** Error de la API con el formato RFC 9457 de la app: título y lista de problemas. */
export class ErrorApi extends Error {
  constructor(titulo, problemas, estado) {
    super(titulo);
    this.titulo = titulo;
    this.problemas = problemas;
    this.estado = estado;
  }
}

async function pedir(url, opciones = {}) {
  let respuesta;
  try {
    respuesta = await fetch(url, opciones);
  } catch {
    throw new ErrorApi('No se pudo conectar con la aplicación', ['Revisa que la app esté corriendo.'], 0);
  }
  if (!respuesta.ok) {
    let cuerpo = null;
    try { cuerpo = await respuesta.json(); } catch { /* respuesta sin JSON */ }
    const problemas = Array.isArray(cuerpo?.problemas) && cuerpo.problemas.length > 0
      ? cuerpo.problemas
      : [cuerpo?.detail ?? `La aplicación respondió con el código ${respuesta.status}.`];
    throw new ErrorApi(cuerpo?.title ?? 'Error', problemas, respuesta.status);
  }
  return respuesta;
}

const json = (r) => r.json();

export const api = {
  /** Plantilla activa con su mapeo, o null si todavía no hay. */
  async plantillaActiva() {
    try {
      return await pedir('api/plantilla/mapeo').then(json);
    } catch (e) {
      if (e instanceof ErrorApi && e.estado === 409) return null;
      throw e;
    }
  },

  subirPlantilla(archivo) {
    const datos = new FormData();
    datos.append('archivo', archivo);
    return pedir('api/plantilla', { method: 'POST', body: datos }).then(json);
  },

  corregirMapeo(hoja, nombreTabla, columnaPorCampoLogico) {
    return pedir('api/plantilla/mapeo', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ hoja, nombreTabla, columnaPorCampoLogico }),
    }).then(json);
  },

  versionesPlantilla() {
    return pedir('api/plantilla/versiones').then(json);
  },

  generarReporte(anio, mes, alestra, telcel, att) {
    const datos = new FormData();
    datos.append('anio', String(anio));
    datos.append('mes', String(mes));
    datos.append('alestra', alestra);
    datos.append('telcel', telcel);
    datos.append('att', att);
    return pedir('api/reportes', { method: 'POST', body: datos }).then(json);
  },

  reportes() {
    return pedir('api/reportes').then(json);
  },

  reporte(id) {
    return pedir(`api/reportes/${encodeURIComponent(id)}`).then(json);
  },

  urlDescarga(id) {
    return `api/reportes/${encodeURIComponent(id)}/descargar`;
  },
};
