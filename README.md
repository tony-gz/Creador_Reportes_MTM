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
- **Fase 3 — Web UI**: lista (spec 11), servida por la misma app en
  `http://localhost:8080/`.
- **Fase 4 — En línea** (spec 12): en curso (2/7).
  - La app **ya no guarda archivos ni datos de clientes** (D-023). Cada
    corrida sube la plantilla y los 3 archivos, y devuelve el `.xlsx`.
  - Inicio de sesión con usuario y contraseña (D-024, D-026).
  - Faltan: pantallas del superadmin, registro de actividad y despliegue en
    Render + Neon.

## Cómo compilar y probar

```bash
cd sim-reports
./mvnw test   # 266 tests, 0 fallos
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
| `SIMREPORTS_ADMIN_USUARIO` | primer superadmin; solo se usa si no hay ningún usuario | — |
| `SIMREPORTS_ADMIN_PASSWORD` | su contraseña temporal (mín. 10 caracteres; se cambia al entrar) | — |
| `SIMREPORTS_ADMIN_NOMBRE` | su nombre para mostrar | `Administrador` |
| `SIMREPORTS_COOKIE_SEGURA` | cookie de sesión solo por HTTPS; `false` únicamente para un host sin HTTPS que no sea `localhost` | `true` |
| `SERVER_PORT` | puerto HTTP (variable estándar de Spring Boot) | `8080` |

```bash
cd sim-reports
./mvnw package -DskipTests
SIMREPORTS_DB_URL=jdbc:postgresql://localhost:5432/sim_reports \
SIMREPORTS_DB_USER=sim_reports SIMREPORTS_DB_PASSWORD='***' \
SIMREPORTS_ADMIN_USUARIO=tony SIMREPORTS_ADMIN_PASSWORD='***' \
java -jar target/sim-reports-0.1.0-SNAPSHOT.jar
```

Con la app corriendo, la pantalla está en `http://localhost:8080/` (o el
puerto de `SERVER_PORT`).

El esquema lo crea Flyway al arrancar (`src/main/resources/db/migration`).
En un servidor se usan las mismas variables. La app no guarda archivos:
descarga el reporte y guárdalo tú.

### Endpoints

Todos piden sesión y, si cambian algo, el token CSRF: la cookie `XSRF-TOKEN`
en la cabecera `X-XSRF-TOKEN`. Con curl:

```bash
curl -c c.txt -b c.txt http://localhost:8080/api/sesion/csrf
T=$(awk '$6=="XSRF-TOKEN"{print $7}' c.txt)
curl -c c.txt -b c.txt -H "X-XSRF-TOKEN: $T" -d usuario=tony -d password='***' \
     http://localhost:8080/api/sesion/entrar
T=$(awk '$6=="XSRF-TOKEN"{print $7}' c.txt)   # el token cambia al entrar
```

Después, en cada llamada: `-c c.txt -b c.txt -H "X-XSRF-TOKEN: $T"`.

```bash
curl -F plantilla=@CONSUMOS_SEPTIEMBRE_2026.xlsx \
     http://localhost:8080/api/plantilla/revisar      # cómo lee la app la plantilla (no la guarda)
curl -F anio=2026 -F mes=10 -F plantilla=@CONSUMOS_SEPTIEMBRE_2026.xlsx \
     -F alestra=@"Tráfico por línea.xlsx" -F telcel=@Uso_de_datos.csv -F att=@att.xlsx \
     http://localhost:8080/api/reportes               # genera; el .xlsx viene en Base64 (xlsxBase64)
```

Si algo tiene problemas, la respuesta es `400` con la lista completa en
`problemas` (formato estándar `application/problem+json`).

## Documentación

Los specs `00-*` a `05-*` describen visión, mapeo de columnas, parsers,
motor de coincidencia, modelo de datos/API y el flujo de trabajo.
`06-checklist-fase1.md` y `08-checklist-fase2.md` llevan el progreso.
`07-api-fase1.md` es el contrato público del motor para la Fase 2.
`09-analisis-octubre-2026.md` compara el motor contra el reporte real de
octubre y lista las decisiones pendientes; `10-registro-decisiones.md`
guarda las decisiones de negocio ya tomadas.
`11-fase3-ui.md` describe la pantalla web y lleva su checklist.
`12-fase4-despliegue.md` lleva la Fase 4 (en línea, usuarios, sin datos de clientes).

Los archivos reales de clientes van en `archivos/` (raíz), que está en
`.gitignore`: nunca se suben al repo.
