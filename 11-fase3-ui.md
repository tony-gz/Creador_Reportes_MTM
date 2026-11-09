# 11 — Fase 3: pantalla web

> Mismo formato que los checklists 06 y 08. Las pantallas consumen la API
> REST de la Fase 2 (spec 04, "Formatos").

## Decisiones
- **HTML + JavaScript dentro del jar** (decisión del usuario, 2026-11-09):
  - páginas estáticas en `src/main/resources/static/`, servidas por la misma
    app Spring Boot;
  - módulos ES nativos, sin Node, sin paso de compilación y sin librerías
    externas ni CDN, para que funcione sin internet;
  - un solo `.jar`.
- **Un solo usuario**, sin inicio de sesión (spec 00).
- **Todo dato del servidor se pinta con `textContent`**, nunca con
  `innerHTML`: nombres de hoja, unidades o mensajes de los archivos podrían
  traer HTML.
- **Errores:** se muestra el `title` y la lista `problemas` que trae toda
  respuesta de error de la API.
- Español, formato de números y fechas `es-MX`. Tema claro y oscuro según
  el sistema.

## Pantallas
1. **Generar reporte** (`#generar`, pantalla inicial):
   - muestra la plantilla activa, o un aviso si no hay;
   - pide el periodo (mes y año; por defecto, el mes anterior) y los 3
     archivos (Alestra, Telcel, ATT);
   - genera el reporte y muestra el resultado (pantalla 4).
2. **Plantilla** (`#plantilla`):
   - versión activa;
   - subir una plantilla nueva, mostrando **todos** los problemas si la
     rechaza;
   - tabla del mapeo por hoja y tabla, con la opción de corregirlo
     (`PUT /api/plantilla/mapeo`);
   - historial de versiones.
3. **Historial** (`#historial`): corridas anteriores, con su detalle y su
   descarga.
4. **Detalle de un reporte** (`#reporte/{id}`, y tras generar):
   - descarga del `.xlsx`;
   - totales por fuente y consistencia;
   - SIMs repetidas (en la misma hoja y entre hojas);
   - **consumo sin cliente** (lista, D-011);
   - avisos;
   - archivos usados;
   - texto completo del reporte final, con botón para copiarlo.

## Checklist
- [x] 1. Estructura: `index.html`, `css/app.css`, módulos `js/` (cliente de
      la API, formato, vistas) y navegación por `#hash`.
- [x] 2. Pantalla Generar reporte.
- [x] 3. Pantalla Plantilla (subir, mapeo, corrección, versiones).
- [x] 4. Pantalla Historial y detalle de reporte.
- [x] 5. Test de que la app sirve la página y sus archivos; prueba manual
      con la app corriendo y los archivos reales. *Falta que el usuario
      pruebe en su navegador los flujos con clics: subir archivos y corregir
      el mapeo.*

**Entregado:** 2026-11-09. **`./mvnw test`: 266/266 OK.**

## Notas de la entrega (2026-11-09)
- **Archivos:** `static/index.html`, `css/app.css` y los módulos de `js/`:
  - `api.js`: cliente de la API y `ErrorApi`;
  - `dom.js`: `el()` construye nodos sin interpretar HTML; también cajas de
    aviso y tablas;
  - `formato.js`;
  - `detalle.js`;
  - `vista-generar.js`, `vista-plantilla.js`, `vista-historial.js`;
  - `app.js`: la navegación.
- **`PaginaWebTest`** comprueba que `/` sirve la página y cada módulo, y que
  ningún módulo usa `innerHTML`, `outerHTML`, `insertAdjacentHTML` ni
  `document.write`.
- **Prueba con la app corriendo** (`.jar` con variables de entorno,
  PostgreSQL temporal, datos reales de octubre cargados por la API) y
  capturas en Firefox sin ventana. Se ven bien con datos reales:
  - generar, con la plantilla activa;
  - plantilla: 19 tablas con sus columnas y correcciones, p. ej.
    `SIM ← Carrier`;
  - historial;
  - detalle: totales que cuadran, 4 SIMs repetidas dentro de una hoja y 1
    entre hojas, 40 líneas y 10 ICCID sin cliente, archivos usados y texto
    del reporte final;
  - un error 404.
- **Pendiente de probar en un navegador real:** los flujos con clics
  (subir plantilla, generar con los 3 archivos, corregir el mapeo). Las
  capturas no ejecutan clics.
