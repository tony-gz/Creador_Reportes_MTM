// Formato de números, fechas y nombres para mostrar (es-MX).

const numero = new Intl.NumberFormat('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const entero = new Intl.NumberFormat('es-MX');
const fecha = new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeStyle: 'short' });

export const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto',
  'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

export const mb = (valor) => `${numero.format(valor ?? 0)} MB`;
export const cantidad = (valor) => entero.format(valor ?? 0);
export const fechaHora = (iso) => (iso ? fecha.format(new Date(iso)) : '');
export const periodo = (anio, mes) => `${MESES[mes - 1] ?? mes} ${anio}`;

const TIPOS = {
  SIMPLE_CASCADA: 'Línea o ICCID',
  SIMPLE_LINEA: 'Por línea',
  SIMPLE_ICCID: 'Por ICCID',
  DUAL_SIM: 'Router (2 líneas)',
};
export const tipoTabla = (tipo) => TIPOS[tipo] ?? tipo;

const FUENTES = { PLANTILLA: 'Plantilla', ALESTRA: 'Alestra', TELCEL_ATT: 'Telcel + ATT', TELCEL: 'Telcel', ATT: 'ATT' };
export const fuente = (clave) => FUENTES[clave] ?? clave;

/** "fila 13", o "fila 12 (LINEA_2)" para la segunda línea de un router. */
export function fila(aparicion) {
  const texto = aparicion.filaExcel == null ? 'fila ?' : `fila ${aparicion.filaExcel}`;
  return aparicion.campo?.startsWith('LINEA_') ? `${texto} (${aparicion.campo})` : texto;
}

/** Periodo por defecto: el mes anterior al actual. */
export function mesAnterior(hoy = new Date()) {
  const d = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
  return { anio: d.getFullYear(), mes: d.getMonth() + 1 };
}
