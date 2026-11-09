// Construcción del DOM sin interpretar texto como HTML: todo texto entra como nodo de texto,
// así un nombre de hoja o un mensaje con "<...>" nunca se interpreta como HTML.

/**
 * el('tag', {atributos}, ...hijos). Atributos: class, dataset, on<evento>
 * (función), booleanos (true = presente). Hijos: nodos o texto; null/false se
 * ignoran; los arreglos se aplanan.
 */
export function el(etiqueta, atributos = {}, ...hijos) {
  const nodo = document.createElement(etiqueta);
  for (const [clave, valor] of Object.entries(atributos ?? {})) {
    if (valor == null || valor === false) continue;
    if (clave === 'class') nodo.className = valor;
    else if (clave === 'dataset') Object.assign(nodo.dataset, valor);
    else if (clave.startsWith('on') && typeof valor === 'function') nodo.addEventListener(clave.slice(2), valor);
    else if (valor === true) nodo.setAttribute(clave, '');
    else nodo.setAttribute(clave, String(valor));
  }
  for (const hijo of hijos.flat(Infinity)) {
    if (hijo == null || hijo === false) continue;
    nodo.append(hijo instanceof Node ? hijo : String(hijo));
  }
  return nodo;
}

/** Reemplaza el contenido de un contenedor. */
export function poner(contenedor, ...hijos) {
  contenedor.replaceChildren(...hijos.flat(Infinity).filter((h) => h != null && h !== false));
}

/** Caja de error con el título y la lista de problemas de un ErrorApi (o de cualquier Error). */
export function cajaError(error, titulo) {
  const problemas = error?.problemas ?? [error?.message ?? String(error)];
  return el('div', { class: 'aviso aviso-error', role: 'alert' },
    el('strong', {}, titulo ?? error?.titulo ?? 'Error'),
    problemas.length === 1 && !titulo
      ? el('span', {}, problemas[0])
      : el('ul', {}, problemas.map((p) => el('li', {}, p))));
}

export function caja(tipo, titulo, ...contenido) {
  return el('div', { class: `aviso aviso-${tipo}`, role: tipo === 'error' ? 'alert' : 'status' },
    titulo ? el('strong', {}, titulo) : null, ...contenido);
}

/** Tabla simple: columnas [{titulo, numero?}], filas = arreglo de arreglos de celdas (texto o nodo). */
export function tabla(columnas, filas, { alta = false } = {}) {
  return el('div', { class: `tabla-contenedor${alta ? ' alta' : ''}` },
    el('table', {},
      el('thead', {}, el('tr', {}, columnas.map((c) => el('th', { class: c.numero ? 'numero' : null, scope: 'col' }, c.titulo)))),
      el('tbody', {}, filas.map((fila) => el('tr', {}, fila.map((celda, i) =>
        el('td', { class: [columnas[i]?.numero ? 'numero' : '', columnas[i]?.mono ? 'mono' : ''].join(' ').trim() || null }, celda)))))));
}
