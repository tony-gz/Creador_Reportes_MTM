# 07 — API de la Fase 1 (motor de parsing puro) para Fase 2

> Documento de contratos, no de diseño. Resume lo que ya existe en
> `sim-reports/src/` (Fase 1: motor de parsing puro, sin Spring) para que
> Claude pueda escribir la Fase 2 (capa Spring Boot, spec 04) **sin que le
> pasen el código fuente completo**. Las specs 00–06 siguen siendo la
> referencia de diseño; este archivo solo describe la API pública que
> consume la Fase 2.

## Estado del proyecto (Fase 1 cerrada)

- Checklist spec 06: **8/8 puntos `[x]`**, todos con `mvn test` en verde.
- Último resultado: `mvn test` → **BUILD SUCCESS, 165 tests, 0 fallos**
  (2026-11-05; incluye Fase 2 punto 1 y las correcciones de la revisión).
- `pom.xml`: `com.simreports:sim-reports:0.1.0-SNAPSHOT`, parent Spring
  Boot 4.1.1 (JUnit/surefire gestionados por el parent),
  `maven.compiler.release=21`, Apache POI 5.5.1 (scope normal: `poi` +
  `poi-ooxml`; >= 5.4.0 por CVE-2025-31672).
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

**`NormalizadorClave`** — clase final, única forma canónica de una clave
(línea o ICCID). La usan los parsers al construir sus `Map` y el motor
sobre las claves de `FilaPlantilla`.
```java
static String normalizar(String valor);              // null → ""
static String quitarEspaciosExtremos(String valor);  // incluye NBSP, BOM, ancho cero
```
Quita espacios (también invisibles), comillas simples envolventes, sufijo
`.0` de un entero, guiones y paréntesis; pasa a mayúsculas; quita la `F`
de relleno de un ICCID de 19 dígitos. **No** quita
prefijos de país (`+52`, `52`, `521`). Idempotente.

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
- **`AttHeaderResolver`**: `ICCID→ICCID`, `CONSUMO_MB→CONSUMO`,
  `USODECICLOHASTALAFECHA(MB)→CONSUMO` (encabezado real de Jasper).
- Telcel/ATT leen **varios bloques** ICCID + consumo lado a lado en la fila
  de encabezado (formato BD_ATT); una fila vacía dentro de un bloque se
  ignora sin aviso.
- **`ParserIccidConsumoGenerico`** (package-private, motor compartido de
  Telcel/ATT): no es parte de la API pública; la Fase 2 no lo usa directo.
  Si el `.csv` no es UTF-8 válido, se lee como Windows-1252.
- **`LectorNumero`** (package-private): consumo escrito como texto en los
  tres parsers. Acepta `1234.5`, `1,234.5`, `1.234,5`, `1,5`, `0,004`; el
  caso ambiguo `1,234` se lee como mil doscientos treinta y cuatro;
  `NaN`/`Infinity`/`10d` → fila excluida + aviso.

### com.simreports.core.matching

**`FuenteConsumo`** — `enum { ALESTRA, TELCEL_ATT }` (Telcel+ATT son una
sola fuente lógica).

**`Asignacion(double consumoMb, FuenteConsumo fuente)`** — record.
- `sinMatch()` estático → `(0.0, null)` (nunca `null` de consumo: misma
  semántica que `SI.ERROR(BUSCARV(...),0)`).
- `boolean tieneMatch()` (fuente no nula); `Optional<FuenteConsumo> fuenteOpcional()`.
- Ojo: un match con `0.0` MB reales sigue teniendo fuente no nula → no es
  "no encontrado".

**`FilaPlantilla(String linea, String iccid, String linea1, String linea2, String sim, Integer filaExcel)`**
— record (hay constructor de 4 args con `sim` y `filaExcel` en `null`).
`sim` = valor crudo de la columna `SIM` / `Carrier` (`conSim(String)`); en
las tablas simples decide dónde se busca (D-016, `OperadorPlantilla`). Campos que no
aplican o celdas vacías = `null` (nunca `""`). `filaExcel` = fila de Excel
(1 = primera), solo para el reporte final; `enFila(int)` devuelve una copia
con la fila. El lector de la plantilla siempre debe llenarla.
Factories estáticos: `simplePorLinea(linea)`, `simplePorIccid(iccid)`,
`cascada(linea, iccid)`, `dualSim(linea1, linea2)` (estos dos NO usan
`linea`/`iccid`). Valores como texto (nunca un `double` en notación
científica); el motor les aplica `NormalizadorClave` antes de buscar, y una
clave vacía tras normalizar cuenta como `null`.

**`ResultadoFilaPlantilla`** — `record (Asignacion consumo, Asignacion consumo1, Asignacion consumo2, Double consumoTotal)`.
- `simple(Asignacion)` → llena `consumo` (CONSUMO(MB)); resto `null`.
- `dualSim(Asignacion, Asignacion)` → llena `consumo1`, `consumo2` y
  `consumoTotal` (recalculado, nunca leído); `consumo` es `null`.

**`TablaConFilas(String hoja, String identificador, TipoTablaPlantilla tipo, List<FilaPlantilla> filas)`**
— record, entrada por tabla. `hoja` **sí decide** el matching de SIMs
repetidas (D-001/D-006): dos tablas de la misma hoja llevan el mismo
`hoja`. `identificador` solo para trazabilidad. Constructor de 3 args
(`hoja = identificador`) solo para pruebas. Lista inmutable.

**`ResultadoTabla(String identificador, List<ResultadoFilaPlantilla> filas)`**
— record, en el mismo orden que las filas de entrada.

**`NoEncontrado(FuenteConsumo fuente, String clave, double consumoMb)`** — record.

**`AdvertenciaConsistencia(FuenteConsumo fuente, double totalOrigen, double totalAsignado, double totalNoEncontrado, double totalRepetidoEntreHojas)`**
— record; `double diferencia()` = `totalOrigen - (totalAsignado - totalRepetidoEntreHojas + totalNoEncontrado)`.

**`ResultadoMatching`** — `record (List<NoEncontrado> noEncontrados, List<AdvertenciaConsistencia> advertenciasConsistencia, List<AvisoParseo> avisosParseo, List<TotalesFuente> totales, List<SimRepetida> simsRepetidas)`.
Listas inmutables. `noEncontrados` ordenados por fuente y clave.
`avisosParseo` = heredados de los parsers + generados por el motor
(ReglaAmbigua). `totales` = uno por fuente (origen, asignado, no
encontrado, repetido entre hojas). `simsRepetidas` = claves con consumo
que aparecen 2+ veces en la plantilla, ordenadas por fuente y clave.
`double totalAsignado()` = suma de lo asignado en todas las fuentes. No
incluye los valores a escribir (eso va por `ResultadoTabla`).

**`TotalesFuente(FuenteConsumo fuente, double totalOrigen, double totalAsignado, double totalNoEncontrado, double totalRepetidoEntreHojas)`**
— record (constructor de 4 args con repetido = 0). `totalAsignado` incluye
lo repetido entre hojas (es lo que debe tener el `.xlsx`).

**`SimRepetida(FuenteConsumo fuente, String clave, double consumoMb, List<AparicionSim> apariciones)`**
— record; `hojas()`, `repetidaEnLaMismaHoja()`, `repetidaEntreHojas()`.

**`AparicionSim(String hoja, String tabla, Integer filaExcel, String campo, double consumoAsignado)`**
— record; `campo` = `LINEA`, `ICCID`, `LINEA_1` o `LINEA_2`.

**`ReporteFinalCorrida`** — clase final: `static String generar(ResultadoMatching)`.
Texto con 6 secciones: totales, repetidas en la misma hoja (quién conservó
el consumo y quién quedó en 0), repetidas entre hojas, consumo sin cliente
(lista de líneas/ICCID de mayor a menor MB), consistencia y otros avisos.
La Fase 2 lo muestra al terminar la corrida (D-006, D-011).

**`VerificadorReporteEscrito`** — clase final, verificación independiente
del `.xlsx` ya escrito:
```java
static Optional<DescuadreReporteEscrito> verificar(ResultadoMatching resultado, double totalLeidoDelReporte);
```
`totalLeidoDelReporte` = suma de las celdas `CONSUMO(MB)`, `CONSUMO(MB)_1`
y `CONSUMO(MB)_2` (no `CONSUMO_TOTAL`) releídas del archivo escrito.
Descuadre > 0.01 MB → bug del escritor; el reporte no debe darse por
válido. **Importante:** la consistencia interna de `finalizar()` cuadra por
construcción con la regla default; esta es la comprobación que sí detecta
errores de escritura. Lo repetido entre hojas cuenta como esperado (está en
`totalAsignado`).

**`DescuadreReporteEscrito(double totalEsperado, double totalLeido)`** — record; `diferencia()`.

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
3. Columna `SIM` / `Carrier` (D-016), en tablas simples: `ALESTRA` → solo
   línea; `TELCEL_ATT` → solo ICCID; si la SIM solo está en el otro
   operador, 0 MB + aviso `TipoAviso.CARRIER_NO_COINCIDE`. Con
   `NO_RECONOCIDO` se usa la regla del tipo de tabla (abajo).
   Regla de cascada (spec 03, regla 1): `LINEA` contra Alestra primero; si
   la línea **no existe** en Alestra, `ICCID` contra Telcel+ATT. Si la línea
   existe pero está repetida en la plantilla, la fila la resuelve
   `ReglaAmbigua` y **no** cae a ICCID (aunque la regla deje 0 MB).
4. Si una clave matchea más de una vez en la corrida, la 2ª aparición en
   adelante se delega en `ReglaAmbigua` (interfaz inyectable). Con la
   default: en otra hoja se cuenta completa (D-001); en la misma hoja, 0 MB
   (D-006).

**`ReglaAmbigua`** (interfaz):
```java
ResolucionAmbigua resolver(ContextoFila contexto);
```

**`ContextoFila`** — record `(String clave, FuenteConsumo fuente, double consumoMb, String hoja, String tablaIdentificador, Integer filaExcel, int numeroDeAparicion, boolean yaAparecioEnEstaHoja)`.
`numeroDeAparicion >= 2` forzado (`IllegalArgumentException` si es menor).
`ubicacion()` = `'hoja' fila N` para mensajes.

**`OperadorPlantilla`** — enum `ALESTRA`, `TELCEL_ATT`, `NO_RECONOCIDO`;
`static desde(String valorColumnaSim)` compara solo letras sin distinguir
mayúsculas (`ALESTRA`/`MULTICARRIER` → `ALESTRA`; `TELCEL`/`ATT`, `AT&T_2`,
`ATT001` → `TELCEL_ATT`; vacío u otro → `NO_RECONOCIDO`).

**`ResolucionAmbigua`** — record `(Asignacion asignacion, String advertencia)`.
`advertencia` puede ser `null`; `advertenciaOpcional()`.

**`PrimeraEnLaHojaGanaReglaAmbigua`** — default de `ReglaAmbigua` (D-001 y
D-006, spec 10): si la clave ya apareció **en la misma hoja**, asigna 0 MB;
si solo había aparecido en **otras hojas**, asigna el consumo completo. En
los dos casos genera aviso `CLAVE_DUPLICADA`. Es la que usa el constructor
de 3 args del motor; se puede reemplazar en el de 4 args. (Reemplaza a
`PrimeraCoincidenciaGanaReglaAmbigua`, que dejaba en 0 también las
repeticiones entre hojas.)

## Notas de comportamiento que la Fase 2 debe respetar

- Los parsers y el motor son **puros**: no conocen HTTP ni Spring. La
  traducción de excepciones a HTTP vive en la Fase 2 (`@ControllerAdvice`).
- La Fase 2 **no** decide formato por nombre de archivo: los parsers ya
  detectan por contenido. Guardar el `.xlsx` subido con nombre propio y
  pasarlo como `InputStream`/bytes.
- `FilaPlantilla` recibe **texto** (spec 01: nunca un `double` en
  notación científica; un número entero se pasa sin decimales). El motor
  aplica `NormalizadorClave`; el lector de la plantilla no debe limpiar
  por su cuenta con otras reglas. La lectura de las
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

- `./mvnw test` debe seguir en **165 tests, 0 fallos** como base; los tests
  nuevos se suman. No requiere Postgres instalado: los tests de
  persistencia levantan un PostgreSQL embebido (ver README).
- Los contratos de arriba no cambian: si la Fase 2 necesita algo distinto,
  hay que discutirlo primero (spec 05: el código nuevo siempre lo escribe
  Claude).