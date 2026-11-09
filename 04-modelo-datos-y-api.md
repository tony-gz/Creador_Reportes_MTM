# 04 — Modelo de datos, API REST y empaquetado

> Esta spec corresponde a **Fase 2** (capa Spring Boot) — se incluye ya en
> este paquete para que el modelo quede definido antes de empezar a picar
> código, pero no bloquea el arranque de Fase 1.

## Entidades

### PlantillaConfig (ahora versionado de forma inmutable)
| Campo | Tipo | Nota |
|---|---|---|
| id | Long | |
| version | int | autoincremental, **nunca se reutiliza** |
| rutaArchivo | String | ruta en disco del .xlsx de esta versión (no blob en BD) |
| mapeoColumnas | JSON | resultado persistido del resolver (spec 01) para esta versión |
| activa | boolean | solo una fila tiene `activa = true` a la vez |
| fechaCreacion | timestamp | |

Cada vez que el usuario reemplaza la plantilla, se **inserta una fila
nueva** (no se hace `UPDATE` sobre la anterior) y se marca `activa = true`
en la nueva, `activa = false` en la anterior. Motivo: reemplazar en sitio
pierde para siempre el mapeo con el que se generó un reporte pasado; con
versiones inmutables, cualquier reporte histórico puede reabrirse sabiendo
exactamente qué mapeo de columnas se usó, sin guardar backups manuales.

Corregir manualmente el mapeo desde la UI (`PUT /api/plantilla/mapeo`)
actualiza el `mapeoColumnas` de la versión **activa** — no crea una versión
nueva, porque no cambia el archivo, solo la interpretación de sus columnas.

### ReporteGenerado
| Campo | Tipo | Nota |
|---|---|---|
| id | Long | |
| fecha | timestamp | |
| plantillaVersionId | Long (FK → PlantillaConfig.id) | **nuevo** — qué versión de plantilla se usó, para reproducibilidad |
| archivosEntrada | JSON / relación | qué archivo (Alestra/Telcel/ATT) se usó en esta corrida |
| rutaArchivoResultado | String | ruta en disco del .xlsx generado, para poder re-descargarlo después |
| lineasNoEncontradas | JSON | resultado del reporte secundario (spec 03) |
| avisos | JSON | duplicados agregados, claves malformadas (spec 02) e inconsistencias de totales (spec 03) |
| periodoAnio, periodoMes | int | mes y año que reporta (desde V2, 2026-11-09) |
| nombreArchivo | String | `CONSUMOS_<MES>_<AÑO>.xlsx` (D-021) |
| reporteFinal | text | reporte final de la corrida (`ReporteFinalCorrida`); solo en la app, no en el `.xlsx` (D-019) |

## Endpoints REST (borrador)
| Método | Ruta | Función |
|---|---|---|
| `POST` | `/api/plantilla` | Subir una nueva versión de la plantilla (crea fila nueva, la marca activa) |
| `GET` | `/api/plantilla/mapeo` | Ver el mapeo de columnas de la versión activa |
| `PUT` | `/api/plantilla/mapeo` | Corregir manualmente el mapeo de una hoja/tabla de la versión activa |
| `GET` | `/api/plantilla/versiones` | Listar versiones históricas de la plantilla |
| `POST` | `/api/reportes` | Subir las 3 BDs (Alestra, Telcel, ATT) y generar el reporte contra la plantilla activa |
| `GET` | `/api/reportes/{id}/descargar` | Descargar el .xlsx resultado |
| `GET` | `/api/reportes` | Listar historial de corridas |

Manejo de errores: un `@ControllerAdvice` global traduce las excepciones de
parseo (`ArchivoFormatoInvalidoException`, spec 02) a respuestas HTTP 400
con el detalle de qué campo/columna faltó, en vez de dejar pasar
excepciones crudas de Apache POI.

### Formatos (implementados el 2026-11-09, endpoints de plantilla)
- `POST /api/plantilla` — `multipart/form-data`, campo `archivo` (.xlsx).
  `201` →
  `{"version":{"id","version","activa","fechaCreacion"},"mapeo":{"tablas":[{"hoja","nombreTabla","rango","tipo","columnaPorCampoLogico","encabezadosNoReconocidos"}]}}`.
- `GET /api/plantilla/mapeo` — `200` con el mismo cuerpo; `409` si todavía
  no hay plantilla.
- `PUT /api/plantilla/mapeo` — cuerpo
  `{"hoja":"GEPP","nombreTabla":"Tabla3","columnaPorCampoLogico":{"LINEA":"Número","ICCID":"ICCID","CONSUMO(MB)":"CONSUMO(MB)"}}`
  (reemplaza completo el mapeo de esa tabla; la tabla se reclasifica).
  `200` con el mismo cuerpo que el `GET`.
- `GET /api/plantilla/versiones` — `200` →
  `[{"id","version","activa","fechaCreacion"}, ...]`, la más reciente
  primero.
- `POST /api/reportes` — `multipart/form-data`: `anio`, `mes`, `alestra`,
  `telcel`, `att`. `201` + `Location: /api/reportes/{id}` →
  `{"id","fecha","periodoAnio","periodoMes","nombreArchivo","versionPlantilla","urlDescarga","reporteFinal","archivosEntrada":[...],"consumoSinCliente":[...],"avisos":{"totales","advertenciasConsistencia","simsRepetidas","avisosParseo","formulasSinRecalcular"}}`.
- `GET /api/reportes` — `200` → `[{"id","fecha","periodoAnio","periodoMes","nombreArchivo","versionPlantilla","urlDescarga"}]`, la más reciente primero.
- `GET /api/reportes/{id}` — `200` con el mismo detalle que el `POST`; `404` si no existe.
- `GET /api/reportes/{id}/descargar` — `200`, el `.xlsx` con
  `Content-Disposition: attachment; filename="CONSUMOS_<MES>_<AÑO>.xlsx"`.
- **Errores** — `application/problem+json` (RFC 9457):
  `{"title","status","detail","problemas":[...]}`. Plantilla con problemas
  o corrección inválida → `400` con **todos** los problemas; archivo de
  operador inválido, vacío o periodo inválido → `400`; sin plantilla activa
  → `409`; reporte inexistente → `404`; reporte escrito que no cuadra →
  `500`. Toda respuesta de error trae `problemas`.

## Base de datos: decidido — PostgreSQL
- Se usa **PostgreSQL** tanto en local (instancia local o vía Docker) como
  en servidor. Al ser el mismo motor en ambos entornos, ya no hace falta
  una decisión de persistencia diferenciada por ambiente — solo cambia la
  cadena de conexión (host/usuario/password), que es config, no diseño.
- Postgres guarda **metadatos y rutas**, no los archivos `.xlsx` en sí
  (ver "Almacenamiento de archivos" abajo).
- **Decidido (2026-11-09): un único `application.yml` parametrizado por
  variables de entorno**, sin perfiles: `SIMREPORTS_DB_URL`,
  `SIMREPORTS_DB_USER`, `SIMREPORTS_DB_PASSWORD`, `SIMREPORTS_DATOS_DIR` y
  `SERVER_PORT` (ver README).

## Almacenamiento de archivos (nuevo)
- Los `.xlsx` (plantilla por versión, reportes generados) se guardan en el
  **filesystem** (una carpeta por versión/corrida), y solo la **ruta** se
  persiste en Postgres.
- Motivo: meter binarios grandes como blob en Postgres infla la base de
  datos y complica backups/migraciones sin necesidad. Para este volumen
  (reportes mensuales, no miles diarios), archivo en disco + referencia en
  BD es más simple de mantener y operar.
- **Retención:** por ahora no se implementa borrado automático de
  versiones o reportes antiguos — es una decisión consciente (no un
  olvido) hasta que se sepa cuántos reportes realmente se acumulan con el
  uso real del sistema. Revisar esta decisión si el volumen de archivos en
  disco se vuelve un problema operativo.

## Concurrencia sobre la plantilla activa (nuevo)
- El sistema asume **un solo usuario operando a la vez** (ver spec 00), así
  que no se justifica infraestructura de locks distribuidos.
- Para evitar que dos requests simultáneos (p. ej. reemplazar la plantilla
  mientras se está generando un reporte) se pisen, basta con un lock a
  nivel de fila de Postgres: `SELECT ... FOR UPDATE` sobre la fila
  `activa = true` de `PlantillaConfig`, dentro de un método
  `@Transactional`, tanto al leerla para generar un reporte como al
  reemplazarla.
- **Actualizado (2026-11-05):** además del lock, la base de datos
  garantiza la regla con una restricción de exclusión parcial diferida
  (`ex_plantilla_config_una_activa`, migración Flyway V1). El lock solo
  funciona si ya existe una fila activa; en la primera subida no hay nada
  que bloquear, y es la restricción la que impide quedar con dos versiones
  activas. Al ser diferida, el reemplazo (insertar la nueva y desactivar la
  anterior) funciona en cualquier orden dentro de la misma transacción.
- Motivo: es la solución nativa de Postgres para este problema exacto, no
  agrega ninguna dependencia nueva al proyecto, y es proporcional al riesgo
  real (uso ocasional por una persona) en vez de sobre-diseñar para un
  escenario multiusuario que todavía no existe.

## Empaquetado (Fase 4)
- Jar ejecutable (`spring-boot:repackage`).
- Sin perfiles: la diferencia entre local y servidor (datasource y carpeta
  de almacenamiento) va por variables de entorno (decidido el 2026-11-09).
- Docker opcional, solo si se decide desplegar en servidor propio con
  contenedores.

## Frontend (Fase 3 — no detallado aquí)
Pantallas mínimas: carga de plantilla, carga de las 3 BDs, resultado con
resumen (matches / no encontrados / avisos / consistencia de totales) y
botón de descarga. La elección de framework (SPA aparte vs plantillas
server-side) es una decisión abierta que no afecta las specs 01–03.
