# 00 — Visión general: App de reportes de consumo de SIMs

## Objetivo
Automatizar la generación del reporte mensual de consumo de datos de SIMs,
eliminando el proceso manual actual basado en `BUSCARV` sobre una plantilla
Excel de 18 hojas (una por cliente).

## Alcance funcional
1. Cargar y persistir una **plantilla** (.xlsx), versionada de forma
   inmutable (ver `04-modelo-datos-y-api.md`) — cada reemplazo crea una
   versión nueva, nunca se sobrescribe la anterior.
2. Recibir 3 bases de datos de origen, con nombre de archivo arbitrario y
   **formato de archivo variable por operador** (confirmado con archivos
   reales de Fase 0 — ver spec 02):
   - **Alestra**: `.xlsx`, línea + tráfico en MB (encabezado real
     `LINEA` / `Tráfico MB`, con filas de metadata antes de la tabla).
   - **Telcel**: `.csv` o `.xlsx` (exportado de claro-connect), ICCID +
     consumo desglosado por fecha, requiere agregación previa (sumar por
     ICCID). El `.csv` trae varias filas de metadata antes del encabezado
     real y valores entre comillas simples literales.
   - **ATT**: ICCID + consumo, ya consolidado por el usuario (mismo shape
     que Telcel post-agregación).
3. Detectar automáticamente las tablas y columnas de cada hoja de la
   plantilla (tablas nativas de Excel vía `XSSFSheet.getTables()`).
4. Ejecutar el motor de coincidencia (línea → Alestra, ICCID → Telcel+ATT) y
   escribir los consumos encontrados (0 si no hay match).
5. Generar y descargar el reporte general (.xlsx) con el resultado, con
   trazabilidad de qué versión de plantilla se usó.
6. Reporte secundario de "no encontrados" (líneas/ICCIDs de las BDs de
   origen que no aparecen en ninguna hoja de la plantilla).
7. Reporte de **consistencia de totales** (auto-validación matemática del
   matching, ver `03-motor-coincidencia.md`) y de **avisos** (duplicados,
   claves malformadas) generados durante el parsing.

## Fuera de alcance (por ahora)
- Generar automáticamente el archivo ATT desde los múltiples exports de
  Jasper (el usuario sigue consolidándolo manualmente).
- Autenticación/multiusuario real. Se asume **un solo usuario operando el
  sistema a la vez**; el único punto de concurrencia cubierto es el
  reemplazo de la plantilla activa (ver spec 04), no un modelo de permisos.
- Política de borrado/retención de reportes históricos: por decisión
  consciente, **no se borra nada automáticamente** por ahora (ver spec 04).
- Consultar el consumo de los routers en la **API de Teltonika RMS**. Hoy
  el usuario copia a mano el aproximado del RMS para las SIMs de Telcel de
  METROBÚS y DICONSA (D-010). Es una tarea futura (spec 08).

## Stack propuesto
| Capa | Elección | Nota |
|---|---|---|
| Backend | Java + Spring Boot | REST API |
| Lectura/escritura Excel | Apache POI (XSSF) | tablas nativas |
| Persistencia relacional | PostgreSQL | metadatos y versiones — ver `04` |
| Almacenamiento de archivos | Filesystem (disco), ruta referenciada en Postgres | ver `04` — evita blobs pesados en BD |
| Frontend | Por definir (SPA servida por el propio backend, o React+Vite aparte) | **decisión abierta** — no bloquea Fase 0/1 |
| Testing | JUnit | motor de parsing debe tener cobertura alta antes de tocar la capa web |

## Plan de fases
1. **Fase 0 — Descubrimiento y spec** *(este paquete)*: confirmar que las
   tablas de la plantilla son nativas de Excel, reunir 1 plantilla real
   (anonimizada) + 1 archivo de muestra por operador, documentar casos borde,
   y **confirmar con datos reales** las dos reglas que hoy quedan detrás de
   una interfaz `ReglaAmbigua` (duplicados en Alestra, router mixto
   línea/ICCID — ver spec 03).
2. **Fase 1 — Motor de parsing puro** (sin Spring, solo Java + Apache POI +
   JUnit): detección de tablas/columnas, parsers de los 3 orígenes, motor de
   coincidencia, validación de consistencia de totales. Módulo de mayor
   riesgo — madurar antes de la capa web.
3. **Fase 2 — Capa Spring Boot**: entidades versionadas, endpoints, manejo
   global de errores (`@ControllerAdvice`), lock de concurrencia sobre la
   plantilla activa.
4. **Fase 3 — Web UI**: carga de plantilla, carga de BDs, resultado
   (matches / no encontrados / avisos / consistencia), descarga.
5. **Fase 4 — Empaquetado**: jar ejecutable, perfiles `application.yml`
   (local vs servidor), Docker si aplica.

## Documentos de este paquete
- `01-plantilla-mapeo-columnas.md` — estructura de la plantilla y diseño del
  mapeo de columnas por hoja (dato, no código).
- `02-parsers-bd-origen.md` — parsers de Alestra, Telcel y ATT.
- `03-motor-coincidencia.md` — reglas de matching, reporte de no
  encontrados y validación de consistencia.
- `04-modelo-datos-y-api.md` — entidades versionadas, endpoints REST,
  almacenamiento y empaquetado.

## Casos borde documentados (con decisión tomada)
- **ICCID vacío en la plantilla o en una BD de origen** → se excluye de la
  suma/matching y se reporta como aviso (spec 02/03), no rompe el proceso.
- **Línea duplicada dentro de la misma hoja o en Alestra** → se agrega
  (suma), igual que Telcel, y se reporta como aviso (spec 02).
- **La misma línea/ICCID apareciendo en dos hojas distintas de la
  plantilla** → pendiente de confirmar con datos reales si esto ocurre en
  la práctica; si aparece, se resuelve con la misma interfaz `ReglaAmbigua`
  de spec 03.
- **Hoja de la plantilla sin ninguna tabla nativa de Excel** → decisión
  tomada con datos reales de Fase 0: al revisar la plantilla real, 7 de
  las 18 hojas todavía no tenían tabla nativa. El usuario se encarga de
  convertir las 18 hojas a `Table` nativa de Excel. El resolver **no**
  intenta adivinar un rango a partir de encabezados sueltos — si en algún
  momento una hoja llega sin tabla nativa, se lanza un error explícito
  señalando qué hoja falta, en vez de fallar en silencio o construir un
  rango "adivinado" que podría ser incorrecto (ver spec 01).
- **Encabezado de columna con variante no contemplada** (p. ej.
  `"CONSUMO (MB)"` con espacio, encontrado en 3 de las 18 hojas reales) →
  la normalización elimina todos los espacios internos antes de comparar,
  no solo los colapsa, así que estas variantes ya no generan columnas "no
  reconocidas" (ver spec 01).
- **Fila de la tabla con línea o ICCID en blanco** (equipo dado de baja) →
  se ignora esa fila para efectos de matching.
- **Encabezado real de un operador distinto al documentado** (p. ej.
  Alestra usa `"Tráfico MB"`, no `"Consumo(MB)"`) → se resuelve agregando
  el sinónimo real al diccionario de campos lógicos del operador, mismo
  mecanismo de `HeaderResolver` que ya existía para la plantilla (spec 02).
- **Archivo de origen con filas de metadata antes del encabezado real**
  (confirmado en los archivos reales de Alestra y Telcel) → si el archivo
  tiene tabla nativa (caso real de Alestra), se usa esa tabla y el
  problema no existe; si no (caso del `.csv` de Telcel), el parser busca
  la fila de encabezado por contenido reconocido, no asume que es la fila
  1 (ver spec 02).
- **Valores de ICCID/MSISDN envueltos en comillas simples literales**
  (confirmado en el .csv real de Telcel) → se remueven al normalizar el
  valor como texto (ver spec 02).
- **Columna declarada en el encabezado pero sin datos en ninguna fila**
  (`USO (BYTES)` en el archivo real de Telcel) → se ignora, nunca rompe el
  parseo (ver spec 02).

## Fase 0 — cerrada
Los pendientes de esta fase ya se confirmaron con archivos reales
(1 plantilla de agosto 2026, 1 archivo de Alestra, 1 archivo de Telcel).
Los hallazgos quedaron incorporados en las specs 01 y 02. Sigue pendiente
únicamente:
- [x] 1 archivo de muestra de ATT ya consolidado (hoja BD_ATT del reporte de
      octubre 2026, ver spec 09).
- [ ] Export crudo de Telcel (el analizado en spec 09 era una tabla
      dinámica ya preparada).
- [ ] Confirmar si existe algún caso real de router mixto (una SIM por
      línea, otra por ICCID) para ajustar la implementación default de
      `ReglaAmbigua` en spec 03.
