# 08 — Checklist de progreso (Fase 2: capa Spring Boot)

> Mismo formato que 06-checklist-fase1.md. Se actualiza al cerrar cada
> punto; regla de bookkeeping de spec 06 aplica igual aqui (opencode
> puede marcar [x] con fecha y resultado de `mvn test` una vez que pase,
> sin esperar a que Claude dicte el texto).

- [x] 1. Entidades JPA `PlantillaConfig` y `ReporteGenerado` + repositorios
      Spring Data (`PlantillaConfigRepository`, `ReporteGeneradoRepository`).
      **Entregado:** 2026-09-06. **`mvn test`: 123/123 OK** (113 de Fase 1 +
      10 de Fase 2).
- [x] 2. Servicio de plantilla: lectura de tablas nativas con POI
      (`XSSFSheet.getTables()`) -> `ResolverTablaPlantilla` -> persistir
      `mapeoColumnas` en la version activa (cableado pendiente segun spec 07).
      **Entregado:** 2026-11-09. **`./mvnw test`: 236/236 OK.** Ver "Notas
      del punto 2" abajo.
      Requisitos confirmados con el reporte real de octubre (spec 09):
      ignorar hojas `BD_*`; fallar si una hoja tiene filas de encabezado
      **fuera** de sus tablas nativas (caso ALSEA bloque 2), no solo si no
      tiene ninguna tabla. Cada `TablaConFilas` lleva el **nombre de la
      hoja** (el mismo para todas las tablas de una hoja) y cada
      `FilaPlantilla` su **fila de Excel** (`enFila`) y el valor de la
      columna **`SIM` / `Carrier`** (`conSim`, campo lógico `SIM`): la regla
      de SIMs repetidas, el reporte final y la búsqueda por operador
      (D-016) dependen de eso. Encabezados reales con
      espacio, como `CONSUMO (MB)` (KUEHENE NAGEL, PETCO, ANIXTER), deben
      reconocerse.
- [ ] 3. Endpoints de plantilla (`POST /api/plantilla`, `GET`/`PUT
      /api/plantilla/mapeo`, `GET /api/plantilla/versiones`) + lock
      `SELECT ... FOR UPDATE` sobre la fila activa. *El reemplazo con lock
      en una transacción ya lo hace `PlantillaService.registrarNuevaVersion`
      (punto 2); falta la capa web y `PUT` del mapeo.* La regla "una sola
      activa" ya la garantiza la base (restricción diferida, se comprueba
      al commit): el reemplazo debe hacerse en **una sola transacción**
      (insertar la nueva activa + desactivar la anterior, en cualquier
      orden).
- [ ] 4. Servicio de reportes: orquesta los 3 parsers + `MotorCoincidencia`,
      escribe el .xlsx de salida, guarda `ReporteGenerado`. Después de
      escribir, **releer el archivo** y pasar la suma de las columnas de
      consumo a `VerificadorReporteEscrito.verificar(...)`; un descuadre
      significa que el escritor tiene un bug y el reporte no se entrega
      como válido.
      - **Reporte final** (D-006, D-011): mostrar `ReporteFinalCorrida` al
        terminar (UI) y guardarlo con el reporte (hoja de resumen en el
        `.xlsx` y/o en `ReporteGenerado`).
      - **Filas sin coincidencia → 0** (D-015), aunque la plantilla traiga
        un número fijo (RMS de Teltonika, STARLINK) o un texto
        (`DESACTIVADO`, `NA`): ese valor es del mes pasado. Toda celda de
        consumo se escribe con lo que da el motor; el verificador no
        necesita casos especiales. *Sugerencia (no decidida):* listar en el
        reporte final las filas cuyo valor fijo se reemplazó por 0, para que
        el usuario sepa cuáles llenar desde el RMS.
- [ ] 5. Endpoints de reportes (`POST /api/reportes`, `GET
      /api/reportes/{id}/descargar`, `GET /api/reportes`).
- [ ] 6. `@ControllerAdvice` global: traduce `ArchivoFormatoInvalidoException`,
      `PlantillaInvalidaException` (con su lista de problemas) y
      `TablaPlantillaNoReconocidaException` a HTTP 400, y
      `SinPlantillaActivaException` a 409. (`HojaSinTablaException` se
      eliminó en el punto 2.)
- [ ] 7. Config de datasource (perfiles local/servidor) + almacenamiento de
      archivos en filesystem. (Flyway ya está configurado. La carpeta base
      ya existe desde el punto 2: `simreports.almacenamiento.directorio`,
      variable `SIMREPORTS_DATOS_DIR`, por defecto `./datos`. Falta decidir
      perfiles vs. variables de entorno.)

## Tareas futuras (fuera de la Fase 2)
- **API de Teltonika RMS** (D-010): consultar automáticamente el consumo de
  los routers cuyas SIMs de Telcel no vienen en ningún archivo de operador
  (hoy METROBÚS y DICONSA, copiado a mano del RMS). Sería una cuarta
  fuente de consumo.

## Notas de esta entrega (punto 1)
- Spring Boot **4.1.1** como parent (la `4.1.2` original no existe en
  Maven Central, HTTP 404; verificada la última 4.1.x estable). `mvn test`
  en verde con 123 tests.
- Tests con `@SpringBootTest` + `@Transactional` (no `@DataJpaTest`):
  Boot 4 reorganizo paquetes de varias anotaciones de test slice
  (`@EntityScan`, `@WebMvcTest`), y no se pudo confirmar si `@DataJpaTest`
  tambien cambio de paquete. Se prefirio la anotacion nucleo, mas lenta
  pero sin riesgo de import roto que opencode no pueda arreglar (spec 05).
  Revisar si vale la pena volver a `@DataJpaTest` cuando lleguen los
  controllers (punto 3).
- ~~`spring.jpa.hibernate.ddl-auto=update` es temporal~~ — reemplazado el
  2026-11-05 por Flyway + `ddl-auto=validate` (ver abajo).
- `mapeoColumnas`, `archivosEntrada`, `lineasNoEncontradas` y `avisos` son
  JSON serializado por el servicio, no por la entidad — no jsonb nativo de
  Postgres, porque no se consulta dentro del JSON. ~~Se guardaban como
  `@Lob`~~: desde el 2026-11-05 son columnas `text` (en Postgres, `@Lob
  String` se mapeaba a `oid`).

## Correcciones al motor de Fase 1 (2026-11-05, antes del punto 2)
Detectadas en una revisión de código y reproducidas con casos concretos.
Cada una tiene su test de regresión; los 11 tests nuevos fallan contra el
código anterior y pasan con el corregido.
- **Cascada + línea repetida** (`MotorCoincidencia.resolverCascada`): si la
  línea existía en Alestra pero estaba repetida en la plantilla, la
  `ReglaAmbigua` default devolvía una asignación sin fuente y la cascada
  caía a buscar el ICCID en Telcel+ATT, con lo que podía asignar el consumo
  de otro operador. Ahora se cae a ICCID solo si la línea **no existe** en
  Alestra.
- **Fila de totales en Alestra** (`AlestraParser`): se leía como si fuera
  una línea llamada "Total" con el total completo. Ahora se descuenta
  `getTotalsRowCount()`.
- **`.xlsx` de Telcel/ATT** (`ParserIccidConsumoGenerico`): el consumo se
  leía con `DataFormatter` (valor mostrado, redondeado por el formato de
  celda; las fórmulas devolvían su texto). Ahora se usa el valor numérico
  real. Un ICCID guardado como número se excluye con aviso (antes entraba
  como `8.95202E+18`).
- **`.csv` de Telcel/ATT**: lector RFC 4180 en vez de `split(",")`, con
  soporte de comillas dobles y descarte del BOM UTF-8 (antes un CSV de ATT
  guardado como "UTF-8 con BOM" fallaba con `ArchivoFormatoInvalidoException`).
- **`HeaderResolver.normalizar`** y la limpieza de valores ahora eliminan
  NBSP, BOM y espacios de ancho cero.

Resultado: `mvn test` 134/134 OK (123 + 11 nuevos).

## Resto de mejoras de la revisión (2026-11-05)
- **`NormalizadorClave`** (core): una sola limpieza de línea/ICCID para los
  parsers y para las claves de la plantilla (el motor la aplica a
  `FilaPlantilla`). No quita prefijos de país: **pendiente de confirmar con
  datos reales** si la plantilla y Alestra usan la misma longitud de línea.
- **`LectorNumero`**: consumos como texto con coma o punto decimal y
  separador de miles; rechaza `NaN`/`Infinity`/`10d`.
- CSV no UTF-8 → se lee como Windows-1252.
- "No encontrados" en orden estable (fuente, clave).
- **Flyway** (`db/migration/V1__esquema_inicial.sql`) + `ddl-auto=validate`.
  Un cambio de esquema = una migración nueva `V<n>__*.sql`; nunca editar
  una ya aplicada.
- Columnas JSON como `text` (antes `@Lob` → `oid` en Postgres).
- Restricción `ex_plantilla_config_una_activa`: exclusión parcial
  **diferida** — solo una plantilla activa, comprobada al commit.
- Tests de persistencia contra **PostgreSQL 18 real embebido** (zonky
  `embedded-postgres`, sin Docker) en vez de H2. La primera ejecución
  descarga los binarios (~unos MB) desde Maven Central.
- `ResultadoMatching.totales()` + **`VerificadorReporteEscrito`**
  (ver punto 4).
- Apache POI 5.2.5 → **5.5.1** (CVE-2025-31672).

Resultado: `mvn test` **160/160 OK**.

## Regla de SIMs repetidas y reporte final (2026-11-09, antes del punto 2)
Implementa D-001, D-006 y D-011 (spec 10) en el core:
- `TablaConFilas` lleva la **hoja**; `FilaPlantilla` la **fila de Excel**.
- Nueva regla default `PrimeraEnLaHojaGanaReglaAmbigua` (reemplaza a
  `PrimeraCoincidenciaGanaReglaAmbigua`): repetida en otra hoja → consumo
  completo; repetida en la misma hoja → 0. Dos tablas de una hoja cuentan
  como la misma hoja.
- `TotalesFuente` / `AdvertenciaConsistencia` con `totalRepetidoEntreHojas`:
  la consistencia descuenta lo contado de nuevo en otra hoja, pero no lo
  repetido dentro de una hoja (eso sigue siendo un doble conteo).
- `ResultadoMatching.simsRepetidas()` (`SimRepetida` + `AparicionSim`) y
  `ReporteFinalCorrida.generar(...)`: totales, repetidas en la misma hoja,
  repetidas entre hojas, consumo sin cliente (lista), consistencia y avisos.
- Corrida real de octubre con la plantilla oficial: 5 repetidas en la misma
  hoja, 1 entre hojas, 40 líneas de Alestra sin cliente, consistencia
  cuadrada (detalle en spec 09).

Resultado: `./mvnw test` **179/179 OK** (165 + 14).

## Búsqueda por operador según la columna SIM / Carrier (2026-11-09)
Decisión D-016 (spec 10):
- Campo lógico `SIM` en la plantilla (`SIM` o `Carrier`); `FilaPlantilla.sim`.
- `OperadorPlantilla`: Alestra y Multicarrier → solo línea; Telcel y ATT →
  solo ICCID; vacío u otro valor → cascada de siempre. No aplica a dual SIM.
- `TipoAviso.CARRIER_NO_COINCIDE` cuando la fila dice un operador y la SIM
  solo está en el otro (la fila queda en 0); sale en "Otros avisos" del
  reporte final.
- Octubre con la plantilla oficial: mismo resultado que antes (1,227 de
  1,258 celdas iguales al manual), sin avisos de operador.

Resultado: `./mvnw test` **211/211 OK** (179 + 32).

## Notas del punto 2 (2026-11-09)
- **`LectorPlantilla`** (core, puro):
  - `detectar(archivo)` se usa al subir la plantilla. Ignora las hojas
    `BD_*` y las vacías, y clasifica cada tabla nativa.
  - Reúne **todos** los problemas en una `PlantillaInvalidaException`, no
    solo el primero:
    - hoja de clientes sin tabla nativa;
    - encabezados fuera de las tablas (ALSEA bloque 2);
    - tabla que no se puede clasificar;
    - ICCID guardado como número.
  - `leer(archivo, mapeo)` se usa al generar un reporte: lee las filas con
    el mapeo guardado, que el usuario pudo corregir a mano.
  - Lee **todas** las filas de datos, también las que no tienen línea ni
    ICCID (D-015); excluye la fila de totales.
  - Cada `FilaPlantilla` lleva la fila de Excel y la columna `SIM`.
  - `TablaLeida` da el índice de columna de cada campo, que es lo que usará
    el escritor.
- **`HojaSinTablaException` eliminada**: la reemplaza
  `PlantillaInvalidaException`, que reporta varias hojas de una vez.
- **Spring:**
  - `PlantillaService`:
    - `registrarNuevaVersion`: valida, toma el lock, guarda el archivo y
      reemplaza la versión activa en una sola transacción;
    - `mapeoActivo`;
    - `leerActivaParaReporte`: toma el lock de la fila activa, que se
      mantiene durante la transacción de quien lo llama.
  - `AlmacenamientoArchivos`: guarda en
    `plantillas/v<n>/plantilla.xlsx`, con escritura atómica, y guarda rutas
    **relativas** a la carpeta base.
  - `MapeoPlantillaJson`: Jackson 3, con formato
    `{"formato":1,"tablas":[...]}`. Se agregó `spring-boot-starter-jackson`.
- **Plantilla oficial de octubre:** `detectar` encuentra los 8 problemas
  esperados (7 hojas sin tabla y ALSEA fila 36). Se probó con una copia con
  esas zonas convertidas en tablas nativas: 19 tablas y 1,060 filas.
  `detectar` → `leer` → motor da el **mismo reporte final** que la corrida
  de análisis.
- ~~Pendiente: convertir a tabla nativa las 7 hojas y el bloque 2 de
  ALSEA~~ — hecho el mismo día en la plantilla oficial (spec 10,
  "Correcciones en la plantilla", D-017 y D-018). Ahora la app la acepta: 19
  tablas, 1,020 filas, mismo reporte final.
