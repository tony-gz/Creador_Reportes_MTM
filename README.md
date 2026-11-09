# Creador_Reportes_MTM

App para automatizar los reportes mensuales de consumo de datos SIM de MTM.
`sim-reports/` es un proyecto Maven/Java 21 + Spring Boot 4.1 que:

1. Lee las bases de datos de consumo de los operadores (Alestra, Telcel y ATT).
2. Resuelve las tablas de la plantilla de 18 hojas y hace el match de líneas/ICCID.
3. Escribe el reporte mensual (.xlsx) y permite consultarlo vía API/Web.

## Estado

- **Fase 1 — Motor de parsing puro** (`com.simreports.core`, sin Spring):
  completa (spec 06) y corregida tras la revisión del 2026-11-05.
- **Fase 2 — Capa Spring Boot (REST + almacenamiento)**: **completa** (7/7,
  spec 08): entidades y esquema con Flyway, lector de la
  plantilla con versionado en disco, endpoints de la plantilla y de
  reportes, manejo de errores, servicio de reportes (genera el `.xlsx`, lo
  verifica y guarda el reporte final) y configuración por variables de
  entorno.
  Los `.xlsx` se guardan en `SIMREPORTS_DATOS_DIR` (por defecto `./datos`,
  excluida de git).
- **Fase 3 — Web UI**: lista (spec 11). Pantallas para generar reportes,
  administrar la plantilla e historial, servidas por la misma app en
  `http://localhost:8080/`. Falta probar en un navegador los flujos con
  clics.

## Cómo compilar y probar

```bash
cd sim-reports
./mvnw test   # 272 tests, 0 fallos
```

`./mvnw` (Maven Wrapper) descarga Maven 3.9.16 la primera vez, verificando
su suma SHA-256; solo hace falta Java 21 o superior. En Windows: `mvnw.cmd test`.

No hace falta tener PostgreSQL instalado para los tests: los de
persistencia levantan un PostgreSQL 18 embebido (`io.zonky.test:embedded-postgres`),
cuyos binarios se descargan de Maven Central en la primera ejecución.

## Cómo ejecutar la app

Se necesita Java 21+ y un PostgreSQL. La configuración va **solo por
variables de entorno** (no hay perfiles); los valores por defecto están en
`src/main/resources/application.yml`:

| Variable | Qué es | Por defecto |
|---|---|---|
| `SIMREPORTS_DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/sim_reports` |
| `SIMREPORTS_DB_USER` | usuario de la base | `sim_reports` |
| `SIMREPORTS_DB_PASSWORD` | contraseña de la base | `sim_reports` |
| `SIMREPORTS_DATOS_DIR` | carpeta donde se guardan las plantillas y los reportes `.xlsx` | `./datos` |
| `SERVER_PORT` | puerto HTTP (variable estándar de Spring Boot) | `8080` |

```bash
cd sim-reports
./mvnw package -DskipTests
SIMREPORTS_DB_URL=jdbc:postgresql://localhost:5432/sim_reports \
SIMREPORTS_DB_USER=sim_reports SIMREPORTS_DB_PASSWORD='***' \
SIMREPORTS_DATOS_DIR=/ruta/a/datos \
java -jar target/sim-reports-0.1.0-SNAPSHOT.jar
```

Con la app corriendo, la pantalla está en `http://localhost:8080/` (o el
puerto de `SERVER_PORT`).

El esquema lo crea Flyway al arrancar (`src/main/resources/db/migration`).
En un servidor se usan las mismas variables (en el servicio de systemd, en
Docker, etc.). La carpeta de datos contiene archivos reales de clientes:
respáldala junto con la base, porque la base solo guarda las rutas.

### Endpoints de la plantilla

Con la app corriendo (`./mvnw spring-boot:run`, puerto 8080):

```bash
curl -F archivo=@plantilla.xlsx http://localhost:8080/api/plantilla   # subir una versión nueva
curl http://localhost:8080/api/plantilla/mapeo                        # mapeo de la versión activa
curl http://localhost:8080/api/plantilla/versiones                    # historial
curl -X PUT -H 'Content-Type: application/json' \
     -d '{"hoja":"GEPP","nombreTabla":"Tabla3","columnaPorCampoLogico":{"LINEA":"LINEA","CONSUMO(MB)":"CONSUMO(MB)"}}' \
     http://localhost:8080/api/plantilla/mapeo                        # corregir el mapeo de una tabla
```

Si la plantilla tiene problemas, la respuesta es `400` con la lista
completa en `problemas` (formato estándar `application/problem+json`).

### Endpoints de reportes

```bash
curl -F anio=2026 -F mes=10 \
     -F alestra=@"Tráfico por línea.xlsx" -F telcel=@Uso_de_datos.csv -F att=@att.xlsx \
     http://localhost:8080/api/reportes                               # generar (devuelve el detalle y el reporte final)
curl http://localhost:8080/api/reportes                               # historial
curl http://localhost:8080/api/reportes/1                             # detalle de una corrida
curl -OJ http://localhost:8080/api/reportes/1/descargar               # descarga CONSUMOS_OCTUBRE_2026.xlsx
```

## Documentación

Los specs `00-*` a `05-*` describen visión, mapeo de columnas, parsers,
motor de coincidencia, modelo de datos/API y el flujo de trabajo.
`06-checklist-fase1.md` y `08-checklist-fase2.md` llevan el progreso.
`07-api-fase1.md` es el contrato público del motor para la Fase 2.
`09-analisis-octubre-2026.md` compara el motor contra el reporte real de
octubre y lista las decisiones pendientes; `10-registro-decisiones.md`
guarda las decisiones de negocio ya tomadas.
`11-fase3-ui.md` describe la pantalla web y lleva su checklist.

Los archivos reales de clientes van en `archivos/` (raíz), que está en
`.gitignore`: nunca se suben al repo.
