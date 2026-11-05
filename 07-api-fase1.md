# 07 — API de la Fase 1 (motor de parsing puro) para Fase 2

> Documento de contratos, no de diseño. Resume lo que ya existe en
> `sim-reports/src/` (Fase 1: motor de parsing puro, sin Spring) para que
> Claude pueda escribir la Fase 2 (capa Spring Boot, spec 04) **sin que le
> pasen el código fuente completo**. Las specs 00–06 siguen siendo la
> referencia de diseño; este archivo solo describe la API pública que
> consume la Fase 2.

## Estado del proyecto (Fase 1 cerrada)

- Checklist spec 06: **8/8 puntos `[x]`**, todos con `mvn test` en verde.
- Último resultado: `mvn test` → **BUILD SUCCESS, 113 tests, 0 fallos**
  (2026-09-06).
- `pom.xml`: `com.simreports:sim-reports:0.1.0-SNAPSHOT`,
  `maven.compiler.release=21`, JUnit 5.10.2 (test) + surefire 3.2.5,
  Apache POI 5.2.5 (scope normal: `poi` + `poi-ooxml`).
- Paquete base: `com.simreports.core`. **Sin Spring, sin HTTP** en Fase 1:
  los parsers y el motor son código puro y así se testean (JUnit puro).

## Mapa de paquetes

```
src/main/java/com/simreports/core/
├── ResultadoParseo.java, AvisoParseo.java, TipoAviso.java
├── ArchivoFormatoInvalidoException.java, HojaSinTablaException.java
├── OrigenParser.java, HeaderResolver.java
├── plantilla/   → ResolverTablaPlantilla + tipos de tabla
├── parsers/     → AlestraParser, TelcelParser, AttParser
└── matching/    → MotorCoincidencia + ReglaAmbigua
```

## Contratos por clase

### com.simreports.core (raíz)

**`OrigenParser`** (interfaz) — contrato común de los 3 parsers.

```java
ResultadoParseo parsear(InputStream archivo);
```
Pura (no conoce HTTP). Ante archivo malformado lanza
`ArchivoFormatoInvalidoException` (unchecked), nunca devuelve resultado
parcial ni silencioso.

**`ResultadoParseo`** — `record (Map<String, Double> consumos, List<AvisoParseo> avisos)`.
- `consumos`: clave (línea para Alestra, ICCID para Telcel/ATT) → MB ya
  agregados (sin duplicados). Copias inmutables; `null` rechazado.
- `avisos`: nunca `null`; lista vacía `List.of()` si no hubo.

**`AvisoParseo`** — `record (TipoAviso tipo, String clave, String detalle)`.
Campos no nulos. `clave` puede ser cadena vacía (caso ICCID vacío).

**`TipoAviso`** — `enum { CLAVE_DUPLICADA, CLAVE_VACIA_O_MALFORMADA }`.

**Excepciones de dominio (todas unchecked, `RuntimeException`):**
- `ArchivoFormatoInvalidoException(String)` y `(String, Throwable)` — parser
  de origen no reconoce el archivo (faltan columnas obligatorias, no hay
  tabla nativa, no se encontró el encabezado dentro del límite).
- `HojaSinTablaException(String nombreHoja)` + `getNombreHoja()` — una hoja
  de la plantilla no tiene tabla nativa de Excel.
- `TablaPlantillaNoReconocidaException(String)` — (en `core.plantilla`) la
  tabla no se puede clasificar o dos encabezados resuelven al mismo campo.

> Para Fase 2: **estas 3 excepciones son las que traduce el
> `@ControllerAdvice`** (spec 04) a respuestas HTTP claras (p. ej. 400),
> en vez de dejar pasar excepciones crudas de Apache POI.

**`HeaderResolver`** (interfaz) — mapea encabezado real → campo lógico.

```java
Optional<String> resolver(String headerReal);
static String normalizar(String header);   // trim + upper + sin espacios internos (incluye NBSP, BOM, ancho cero); null → ""
```
Nunca lanza por encabezado no reconocido: devuelve `Optional.empty()`.

### com.simreports.core.plantilla

**`PlantillaHeaderResolver`** — implementación para la plantilla. Reconocidos
(normalizados): `LINEA, LINEA_1, LINEA_2, ICCID, CONSUMO(MB), CONSUMO(MB)_1,
CONSUMO(MB)_2`.

**`TipoTablaPlantilla`** — `enum { SIMPLE_LINEA, SIMPLE_ICCID, SIMPLE_CASCADA, DUAL_SIM }`.

**`TablaPlantillaResuelta`** —
`record (TipoTablaPlantilla tipo, Map<String,String> columnaPorCampoLogico, List<String> encabezadosNoReconocidos)`.
- `columnaPorCampoLogico`: campo lógico → **encabezado real sin normalizar**
  (es lo que se usa después para ubicar la columna en POI).
- `encabezadosNoReconocidos`: no bloquean, se persisten para revisión en UI.

**`ResolverTablaPlantilla`** — constructores `()`, `(HeaderResolver)`.
```java
TablaPlantillaResuelta resolver(List<String> headersReales);
```
Puro, no sabe de Excel. Recibe lista de encabezados de una tabla ya
detectada y clasifica. `TablaPlantillaNoReconocidaException` si dos
encabezados resuelven al mismo campo lógico o no alcanzan las columnas
para clasificar.

> Para Fase 2: el `mapeoColumnas` persistido en `PlantillaConfig` (spec 04)
> se construye a partir de `TablaPlantillaResuelta` (campos + encabezados
> reales). La lectura real de tablas vía `XSSFSheet.getTables()` aún no
> está cableada — hay que conectarla en Fase 2/3.

### com.simreports.core.parsers

Tres implementaciones de `OrigenParser`. Cada una: constructores `()` y
`(HeaderResolver)`. Uso típico en Fase 2: `new AlestraParser()` etc., una
instancia por archivo subido.

- **`AlestraParser`**: usa la **primera tabla nativa** de `getTables()` de
  cualquier hoja del libro (el archivo real trae metadata arriba). Espera
  columnas `LINEA` y `Tráfico MB` (sinónimos en `AlestraHeaderResolver`).
  Duplicados de línea → **se suman** + aviso `CLAVE_DUPLICADA`. Línea vacía
  → excluida + aviso. Sin tabla nativa → `ArchivoFormatoInvalidoException`.
  La **fila de totales** de la tabla nativa (si está activada) se ignora.
- **`AlestraHeaderResolver`**: `LINEA→LINEA`, `TRÁFICOMB→CONSUMO`,
  `TRAFICOMB→CONSUMO`.
- **`TelcelParser`**: soporta `.csv` y `.xlsx` detectados **por contenido**
  (firma ZIP `PK` = xlsx, si no CSV), nunca por nombre de archivo. Busca la
  fila de encabezado real dentro de las **primeras 20 filas** (metadata
  previa). Solo lee `ICCID` y `USO (MB)`; ignora el resto. Remueve comillas
  simples literales `'...'`. Agrega por ICCID. **No** genera
  `CLAVE_DUPLICADA` (una fila por fecha es la forma normal). ICCID vacío →
  excluido + aviso.
  - `.csv`: lector RFC 4180 (campos entre comillas dobles con comas o saltos
    de línea dentro, `""` escapada, fin de línea `\n`/`\r\n`/`\r`), descarta
    el BOM UTF-8 inicial; comilla doble sin cerrar →
    `ArchivoFormatoInvalidoException`.
  - `.xlsx`: el consumo se toma del **valor numérico real** de la celda (o
    del resultado cacheado si es fórmula), no del texto mostrado por
    `DataFormatter`. Un ICCID guardado como **número** (ya dañado en el
    archivo: un `double` no conserva 19-20 dígitos) → excluido + aviso
    `CLAVE_VACIA_O_MALFORMADA` con los MB afectados.
- **`TelcelHeaderResolver`**: `ICCID→ICCID`, `USO(MB)→CONSUMO`.
- **`AttParser`**: idéntico a Telcel post-agregación. Espera 2 columnas
  `ICCID, Consumo_MB`. Mismas reglas de encabezado vacío.
- **`AttHeaderResolver`**: `ICCID→ICCID`, `CONSUMO_MB→CONSUMO`.
- **`ParserIccidConsumoGenerico`** (package-private, motor compartido de
  Telcel/ATT): no es parte de la API pública; la Fase 2 no lo usa directo.

### com.simreports.core.matching

**`FuenteConsumo`** — `enum { ALESTRA, TELCEL_ATT }` (Telcel+ATT son una
sola fuente lógica).

**`Asignacion(double consumoMb, FuenteConsumo fuente)`** — record.
- `sinMatch()` estático → `(0.0, null)` (nunca `null` de consumo: misma
  semántica que `SI.ERROR(BUSCARV(...),0)`).
- `boolean tieneMatch()` (fuente no nula); `Optional<FuenteConsumo> fuenteOpcional()`.
- Ojo: un match con `0.0` MB reales sigue teniendo fuente no nula → no es
  "no encontrado".

**`FilaPlantilla(String linea, String iccid, String linea1, String linea2)`**
— record. Campos que no aplican o celdas vacías = `null` (nunca `""`).
Factories estáticos: `simplePorLinea(linea)`, `simplePorIccid(iccid)`,
`cascada(linea, iccid)`, `dualSim(linea1, linea2)` (estos dos NO usan
`linea`/`iccid`). Valores ya normalizados a texto (sin `.0` ni notación
científica); el motor compara igualdad exacta.

**`ResultadoFilaPlantilla`** — `record (Asignacion consumo, Asignacion consumo1, Asignacion consumo2, Double consumoTotal)`.
- `simple(Asignacion)` → llena `consumo` (CONSUMO(MB)); resto `null`.
- `dualSim(Asignacion, Asignacion)` → llena `consumo1`, `consumo2` y
  `consumoTotal` (recalculado, nunca leído); `consumo` es `null`.

**`TablaConFilas(String identificador, TipoTablaPlantilla tipo, List<FilaPlantilla> filas)`**
— record, entrada por tabla (identificador solo para trazabilidad, no
decide matching). Lista inmutable.

**`ResultadoTabla(String identificador, List<ResultadoFilaPlantilla> filas)`**
— record, en el mismo orden que las filas de entrada.

**`NoEncontrado(FuenteConsumo fuente, String clave, double consumoMb)`** — record.

**`AdvertenciaConsistencia(FuenteConsumo fuente, double totalOrigen, double totalAsignado, double totalNoEncontrado)`**
— record; `double diferencia()` = `totalOrigen - (totalAsignado + totalNoEncontrado)`.

**`ResultadoMatching`** — `record (List<NoEncontrado> noEncontrados, List<AdvertenciaConsistencia> advertenciasConsistencia, List<AvisoParseo> avisosParseo)`.
Listas inmutables. `avisosParseo` = heredados de los parsers + generados
por el motor (ReglaAmbigua). No incluye los valores a escribir (eso va por
`ResultadoTabla`).

**`CombinadorTelcelAtt`** — clase final con `private` ctor.
```java
static ResultadoParseo combinar(ResultadoParseo telcel, ResultadoParseo att);
```
Mismo ICCID en ambos → se suman + aviso `CLAVE_DUPLICADA`. Es interno del
motor; la Fase 2 no lo llama directo.

**`MotorCoincidencia`** — el consumidor principal de la Fase 2.

```java
MotorCoincidencia(ResultadoParseo alestra, ResultadoParseo telcel, ResultadoParseo att)
MotorCoincidencia(ResultadoParseo alestra, ResultadoParseo telcel, ResultadoParseo att, ReglaAmbigua reglaAmbigua)
List<ResultadoTabla> resolverTablas(List<TablaConFilas> tablas)  // o resolverTabla(...) por tabla
ResultadoMatching finalizar()
```
**Reglas de uso (importantes):**
1. Una **instancia por corrida** (una subida de las 3 BDs + la plantilla).
   No reutilizar entre corridas (acumula estado interno).
2. **Dos fases, en orden**: (a) `resolverTablas`/`resolverTabla` sobre TODAS
   las tablas de las 18 hojas, guardando cada `ResultadoTabla` para escribirlo
   después en el Excel de salida; (b) `finalizar()` exactamente **una vez**
   para obtener "no encontrados" + consistencia + avisos. Llamar
   `finalizar()` antes de recorrer todas las tablas da "no encontrados"
   incompletos.
3. Regla de cascada (spec 03, regla 1): `LINEA` contra Alestra primero; si
   la línea **no existe** en Alestra, `ICCID` contra Telcel+ATT. Si la línea
   existe pero está repetida en la plantilla, la fila la resuelve
   `ReglaAmbigua` y **no** cae a ICCID (aunque la regla deje 0 MB).
4. Si una clave matchea más de una vez en la corrida, la 2ª aparición en
   adelante se delega en `ReglaAmbigua` (interfaz inyectable).

**`ReglaAmbigua`** (interfaz):
```java
ResolucionAmbigua resolver(ContextoFila contexto);
```

**`ContextoFila`** — record `(String clave, FuenteConsumo fuente, double consumoMb, String tablaIdentificador, int numeroDeAparicion)`.
`numeroDeAparicion >= 2` forzado (`IllegalArgumentException` si es menor).

**`ResolucionAmbigua`** — record `(Asignacion asignacion, String advertencia)`.
`advertencia` puede ser `null`; `advertenciaOpcional()`.

**`PrimeraCoincidenciaGanaReglaAmbigua`** — default de `ReglaAmbigua`: a
partir de la 2ª aparición asigna 0 MB y genera aviso `CLAVE_DUPLICADA` con
clave, tabla y consumo que habría correspondido. Es la implementación que
usa el constructor de 3 args del motor; se puede reemplazar por otra en el
constructor de 4 args (spec 03: el ajuste por datos reales de Fase 0 se
limita a esta clase).

## Notas de comportamiento que la Fase 2 debe respetar

- Los parsers y el motor son **puros**: no conocen HTTP ni Spring. La
  traducción de excepciones a HTTP vive en la Fase 2 (`@ControllerAdvice`).
- La Fase 2 **no** decide formato por nombre de archivo: los parsers ya
  detectan por contenido. Guardar el `.xlsx` subido con nombre propio y
  pasarlo como `InputStream`/bytes.
- El motor exige **texto normalizado** en `FilaPlantilla` (spec 01:
  `DataFormatter`, sin `.0` ni notación científica). La lectura de las
  celdas de la plantilla con POI se conecta en Fase 2/3: por cada tabla
  nativa (18 hojas) → encabezados reales → `ResolverTablaPlantilla` →
  filas → `TablaConFilas`.
- `CONSUMO_TOTAL` en tablas dual SIM **siempre se recalcula** (lo hace
  `ResultadoFilaPlantilla.dualSim`); nunca se lee del Excel.
- Almacenamiento (spec 04): los `.xlsx` (plantillas versionadas y
  reportes) van al filesystem y solo la ruta se persiste en Postgres.
- Entidades de Fase 2 (`PlantillaConfig`, `ReporteGenerado`) y endpoints:
  ver spec 04. El `mapeoColumnas` de `PlantillaConfig` se debe poder
  construir/rehidratar desde `TablaPlantillaResuelta` (JSON).

## Cómo verificar la Fase 2

- `mvn test` debe seguir en **113 tests, 0 fallos** con la Fase 1 intacta;
  los tests de Fase 2 se suman a esa base.
- Los contratos de arriba no cambian: si la Fase 2 necesita algo distinto,
  hay que discutirlo primero (spec 05: el código nuevo siempre lo escribe
  Claude).