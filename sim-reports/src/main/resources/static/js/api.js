// Cliente de la API REST (spec 04, "Formatos"; D-023 en spec 12).
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
    throw new ErrorApi('No se pudo conectar con la aplicación', ['Revisa tu conexión a internet.'], 0);
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
