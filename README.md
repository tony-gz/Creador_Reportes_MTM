# Creador_Reportes_MTM

App para automatizar los reportes mensuales de consumo de datos SIM de MTM.
`sim-reports/` es un proyecto Maven/Java 21 + Spring Boot 4.1 que:

1. Lee las bases de datos de consumo de los operadores (Alestra, Telcel y ATT).
2. Resuelve las tablas de la plantilla de 18 hojas y hace el match de líneas/ICCID.
3. Escribe el reporte mensual (.xlsx) y permite consultarlo vía API/Web.

## Estado

- **Fase 1 — Motor de parsing puro** (`com.simreports.core`, sin Spring):
  completa (spec 06) y corregida tras la revisión del 2026-11-05.
- **Fase 2 — Capa Spring Boot (REST + almacenamiento)**: en curso (spec 08).
  Puntos 1 a 6 de 7 listos: entidades, repositorios, esquema con Flyway,
  lector de la plantilla con versionado en disco, endpoints de la
  plantilla y de reportes, manejo de errores y servicio de reportes (genera
  el `.xlsx`, lo verifica y guarda el reporte final). Queda el punto 7
  (perfiles de configuración).
  Los `.xlsx` se guardan en `SIMREPORTS_DATOS_DIR` (por defecto `./datos`,
  excluida de git).
- **Fase 3 — Web UI**: pendiente.

## Cómo compilar y probar

```bash
cd sim-reports
./mvnw test   # 263 tests, 0 fallos
```

`./mvnw` (Maven Wrapper) descarga Maven 3.9.16 la primera vez, verificando
su suma SHA-256; solo hace falta Java 21 o superior. En Windows: `mvnw.cmd test`.

No hace falta tener PostgreSQL instalado para los tests: los de
persistencia levantan un PostgreSQL 18 embebido (`io.zonky.test:embedded-postgres`),
cuyos binarios se descargan de Maven Central en la primera ejecución.

Para **ejecutar** la app sí se necesita un PostgreSQL. La conexión se
configura con `SIMREPORTS_DB_URL`, `SIMREPORTS_DB_USER` y
`SIMREPORTS_DB_PASSWORD` (valores por defecto en
`src/main/resources/application.yml`). El esquema lo crea Flyway al
arrancar (`src/main/resources/db/migration`).

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

Los archivos reales de clientes van en `archivos/` (raíz), que está en
`.gitignore`: nunca se suben al repo.
