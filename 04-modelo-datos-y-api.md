# 04 — Modelo de datos, API REST y empaquetado

> **Actualizado el 2026-11-09 por D-023 (spec 12):** la app **no guarda
> archivos ni datos de clientes**.
> - Ya no existen las versiones de plantilla (`PlantillaConfig`), el mapeo
>   guardado y su corrección, el historial de reportes (`ReporteGenerado`)
>   ni la carpeta de archivos en disco. La migración V3 borra esas tablas.
> - El diseño anterior está en el historial de git (hasta el commit
>   `e0b6dfd`).
> - PostgreSQL queda para los usuarios (V4) y el registro de actividad
>   (Fase 4, puntos 2 a 4).

## Entidades
### Usuario (V4, Fase 4 punto 2 — spec 12)
| Campo | Tipo | Nota |
|---|---|---|
| id | Long | |
| nombreUsuario | String | minúsculas, único, 3–50 de `a-z 0-9 . _ -` |
| nombreCompleto | String | para mostrar |
| passwordHash | String | BCrypt |
| passwordActualizado | timestamp | último cambio de contraseña |
| versionCredenciales | int | sube con cada cambio: cierra las demás sesiones |
| rol | `SUPERADMIN` / `USUARIO` | D-024 |
| activo | boolean | |
| debeCambiarPassword | boolean | contraseña temporal |
| intentosFallidos, bloqueadoHasta | int, timestamp | 5 fallos → 15 min (D-026) |
| fechaCreacion, ultimoAcceso | timestamp | |

## Endpoints REST
Todos piden sesión salvo `GET /api/sesion/csrf` y `POST /api/sesion/entrar`.
Los que cambian algo piden el token CSRF (cookie `XSRF-TOKEN`, cabecera
`X-XSRF-TOKEN`).

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/api/sesion/csrf` | Deja la cookie `XSRF-TOKEN` |
| `POST` | `/api/sesion/entrar` | Entrar (`usuario`, `password`) |
| `GET` | `/api/sesion` | Quién soy |
| `POST` | `/api/sesion/password` | Cambiar la contraseña propia (`{"actual","nueva"}`) |
| `POST` | `/api/sesion/salir` | Salir |
| `POST` | `/api/plantilla/revisar` | Ver cómo lee la app una plantilla, sin guardarla |
| `POST` | `/api/reportes` | Subir la plantilla y las 3 BDs (Alestra, Telcel, ATT); devuelve el detalle con el `.xlsx` |

Manejo de errores: un `@RestControllerAdvice` global (`ManejadorErrores`)
traduce las excepciones a respuestas HTTP con el detalle de qué falló, en
vez de dejar pasar excepciones crudas de Apache POI.

### Formatos
- `POST /api/plantilla/revisar` — `multipart/form-data`, campo `plantilla`
  (.xlsx). `200` →
  `{"tablas":[{"hoja","nombreTabla","rango","tipo","columnaPorCampoLogico","encabezadosNoReconocidos"}]}`.
- `POST /api/reportes` — `multipart/form-data`: `anio`, `mes`, `plantilla`,
  `alestra`, `telcel`, `att`. `200` →
  `{"fecha","periodoAnio","periodoMes","nombreArchivo","reporteFinal","archivosEntrada":[{"fuente","nombre","bytes"}],"consumoSinCliente":[...],"avisos":{"totales","advertenciasConsistencia","simsRepetidas","avisosParseo","formulasSinRecalcular"},"xlsxBase64"}`.
  - El `.xlsx` va en Base64 dentro de la respuesta: el navegador lo
    descarga y el servidor no se queda con ninguna copia.
  - `archivosEntrada` incluye la plantilla (`PLANTILLA`).
- Sesión: `entrar` y `GET /api/sesion` → `{"usuario","nombreCompleto","rol","debeCambiarPassword"}`.
  Sin sesión → `401`; con contraseña temporal, todo salvo `/api/sesion*` →
  `403`; usuario desactivado o con contraseña o rol cambiados → `401` y la
  sesión se cierra; sin token CSRF → `403`; `/api/admin/**` sin ser
  superadmin → `403`.
- **Errores** — `application/problem+json` (RFC 9457):
  `{"title","status","detail","problemas":[...]}`. Toda respuesta de error
  trae `problemas`.
  - Plantilla con problemas → `400` con **todos** los problemas.
  - Archivo de operador inválido, archivo vacío o periodo inválido → `400`.
  - Reporte escrito que no cuadra → `500`.

## Base de datos: PostgreSQL
- **PostgreSQL** en local y en servidor (Neon, D-027); solo cambia la
  cadena de conexión.
- **Un único `application.yml` parametrizado por variables de entorno**,
  sin perfiles: `SIMREPORTS_DB_URL`, `SIMREPORTS_DB_USER`,
  `SIMREPORTS_DB_PASSWORD` y `SERVER_PORT` (ver README).
- Esquema con Flyway (`src/main/resources/db/migration`). Hibernate solo lo
  valida.

## Concurrencia
- Cada corrida es independiente y vive solo en memoria: no comparte estado
  con otras, así que no hace falta ningún lock.

## Empaquetado (Fase 4)
- Jar ejecutable (`spring-boot:repackage`).
- Sin perfiles: la diferencia entre local y servidor (datasource) va por
  variables de entorno (decidido el 2026-11-09).
- Docker para Render (D-027, spec 12).

## Frontend (Fase 3 — no detallado aquí)
Pantallas mínimas: carga de plantilla, carga de las 3 BDs, resultado con
resumen (matches / no encontrados / avisos / consistencia de totales) y
botón de descarga. La elección de framework (SPA aparte vs plantillas
server-side) es una decisión abierta que no afecta las specs 01–03.
