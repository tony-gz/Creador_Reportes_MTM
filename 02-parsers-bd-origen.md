# 02 — Parsers de las bases de datos de origen

Los tres archivos llegan con **nombre de archivo arbitrario** (se guardan
con otro nombre al descargarlos) — la app nunca debe depender del nombre
del archivo, solo de su contenido/estructura.

Los tres parsers resuelven sus encabezados usando la misma interfaz
`HeaderResolver` definida en `01-plantilla-mapeo-columnas.md`, cada uno con
su propia lista fija de campos lógicos esperados. Esto evita mantener dos
formas distintas de mapear encabezados en el proyecto y da un
comportamiento uniforme cuando un encabezado no se reconoce (ver
"Validación de formato" más abajo).

## Alestra
- **Formato confirmado con archivo real (Fase 0):** `.xlsx`, 2 columnas,
  encabezados reales `LINEA` / `Tráfico MB` (no `"Consumo (MB)"` como se
  documentaba genéricamente). `"TRÁFICO MB"` normalizado se agrega como
  sinónimo del campo lógico `CONSUMO` en el `HeaderResolver` de este
  parser (misma interfaz definida en spec 01, con su propia lista de
  campos para Alestra).
- **Filas de metadata antes de la tabla:** el archivo real trae 2 filas de
  encabezado de reporte (nombre de filtro, fechas) antes de la tabla real.
  Esto **no requiere lógica especial**, porque el archivo real sí tiene
  tabla nativa de Excel (`Table1`, empieza en la fila donde está
  `LINEA`/`Tráfico MB`) — el parser usa `getTables()` igual que la
  plantilla, y `getTables()` ya da el rango correcto sin importar qué haya
  arriba. Si algún mes llegara un archivo de Alestra sin tabla nativa, se
  aplica la misma regla que en la plantilla: lanzar
  `ArchivoFormatoInvalidoException` en vez de adivinar dónde empieza el
  encabezado.
- **Duplicados (decisión tomada):** si una línea aparece más de una vez en
  el archivo, se **agrega** (`groupBy(Linea).sum(ConsumoMB)`) — misma
  política que Telcel. Se elige sumar y no "quedarse con la última" porque
  la fórmula manual actual (`BUSCARV`) toma el primer match y oculta el
  problema; sumar es la interpretación más segura (nunca se pierden MB
  reales) y reutiliza la misma lógica de agregación que ya existe para
  Telcel, en vez de una regla distinta por operador.
- Cada línea duplicada detectada se agrega a una lista de **avisos** que
  viaja junto al `Map` resultante (no bloquea el proceso, solo informa —
  ver "Contrato de salida" más abajo).
- Salida esperada del parser: `Map<Linea, ConsumoMB>` + lista de avisos.

## Telcel
- Formato de entrada: exportado de claro-connect, **desglosado por fecha**
  — el mismo ICCID puede aparecer en múltiples filas (una por día).
- Columnas reales del archivo (confirmadas con archivo de muestra):
  `DURACIÓN (UTC), ICCID, MSISDN, IMSI, USO (KB), USO (MB), USO (BYTES)`.
  El parser solo lee `ICCID` y `USO (MB)`; el resto de columnas
  (`DURACIÓN (UTC)`, `MSISDN`, `IMSI`, `USO (KB)`, `USO (BYTES)`) se
  ignoran.
- **Formato de archivo (confirmado con archivo real, Fase 0): `.csv` o
  `.xlsx`.** El archivo de muestra llegó como `.csv`, no `.xlsx` como se
  asumía en la spec 00 original. El parser de Telcel debe soportar ambos
  formatos — misma interfaz `OrigenParser`, dos implementaciones de
  lectura de filas (una vía Apache POI para `.xlsx`, otra vía un lector de
  CSV) que alimentan la misma lógica de resolución de encabezados y
  agregación.
- **Filas de metadata antes del encabezado real:** el `.csv` real trae 7
  líneas de metadata del reporte (cuenta, usuario, rango de fechas) antes
  del encabezado verdadero. A diferencia de Alestra, un `.csv` no tiene
  concepto de tabla nativa, así que aquí el parser **sí** necesita buscar
  la fila de encabezado por contenido: recorre las primeras N filas (con
  un límite razonable, p. ej. 20) buscando la primera que, tras normalizar
  cada celda con el mismo `HeaderResolver`, resuelva al menos los campos
  obligatorios (`ICCID`, `USO (MB)`). Si no la encuentra dentro del
  límite, lanza `ArchivoFormatoInvalidoException`.
- **Valores entre comillas simples literales:** en el `.csv` real, `ICCID`
  y `MSISDN` llegan como `'8952020221396597611'` — las comillas son parte
  del texto de la celda (no un delimitador CSV), típico de exports que
  fuerzan formato texto. El parser debe remover comillas simples al
  inicio/final del valor como parte de la misma normalización a texto que
  ya aplica `DataFormatter` para `.xlsx` (ver más abajo).
- **Columna sin datos:** en el archivo real, `USO (BYTES)` aparece en el
  encabezado pero ninguna fila trae valor en esa posición (todas las filas
  reales solo traen 6 de los 7 campos). El parser indexa columnas **por
  nombre de encabezado**, no por posición fija, y trata cualquier columna
  no usada por el contrato de salida (`ICCID`, `USO (MB)`) como
  irrelevante — nunca falla porque una fila tenga menos columnas que el
  encabezado.
- Transformación requerida antes de pasar al motor de coincidencia:
  `groupBy(ICCID).sum(USO (MB))`.
- Salida esperada del parser: `Map<ICCID, ConsumoMB>` — mismo shape que
  ATT, ya agregado.
- Caso borde: ICCID vacío o malformado en alguna fila → se excluye de la
  suma y se agrega a la lista de avisos (no rompe el proceso completo).

## ATT
- Formato de entrada: **un solo archivo**, ya consolidado por el usuario a
  partir de varios exports de Jasper, con exactamente 2 columnas:
  `ICCID, Consumo_MB`.
- El parser es **idéntico al de Telcel post-agregación** — no necesita
  lógica de agregación propia, solo leer esas dos columnas.
- Salida esperada: `Map<ICCID, ConsumoMB>` + lista de avisos (ICCID vacío o
  malformado, mismo criterio que Telcel).

## Validación de formato de archivo
Cada parser debe fallar de forma **explícita y temprana**, no en silencio,
cuando el archivo no tiene la forma esperada:
- Si, tras resolver encabezados con `HeaderResolver`, faltan campos lógicos
  obligatorios (p. ej. no aparece `ICCID` ni `USO (MB)` en ningún
  encabezado, o no se encontró tabla nativa en el caso de Alestra), el
  parser lanza una excepción propia `ArchivoFormatoInvalidoException`,
  indicando qué campos esperaba y qué encabezados encontró.
- Esta excepción es capturada en la capa web (Fase 2) por un manejador
  global (`@ControllerAdvice`) que la traduce a una respuesta HTTP clara
  para el usuario (p. ej. 400 con el mensaje del campo faltante), en vez de
  dejar pasar una excepción cruda de Apache POI o un `NullPointerException`.
- Los parsers siguen siendo **puros** (sin conocer HTTP): solo lanzan la
  excepción, la traducción a respuesta HTTP vive en la capa web. Esto
  mantiene la testabilidad con JUnit puro definida en Fase 1.
- Motivo de resolverlo así: si mañana un operador cambia el nombre de una
  columna en su export, el usuario ve un mensaje ("no encontré la columna
  ICCID/USO (MB) en este archivo") en vez de que el reporte salga con
  ceros silenciosos — el fallo más costoso posible en este dominio.

## Interfaz común
Los 3 parsers deben exponer el mismo contrato de salida hacia el motor de
coincidencia, para que este no necesite saber de qué operador viene cada
dataset:
```java
interface OrigenParser {
    ResultadoParseo parsear(InputStream archivo);
}

record ResultadoParseo(
    Map<String, Double> consumos,
    List<AvisoParseo> avisos   // duplicados agregados, claves vacías/malformadas excluidas
) {}
```
Alestra se indexa por **línea**; Telcel y ATT se indexan por **ICCID** (y
pueden combinarse en un solo `Map<ICCID, ConsumoMB>` antes del matching, o
mantenerse separados si se quiere trazabilidad de qué operador aportó el
consumo — **decisión abierta**, no bloquea el diseño del parser).

### Tipo de dato de celda al leer línea/ICCID (mismo riesgo que en la
plantilla, ver spec 01)
La clave del `Map` (línea o ICCID) siempre debe leerse/tratarse como
**texto**, sin importar cómo Alestra/Telcel/ATT hayan formateado la celda
en su export:
- Un `ICCID` numérico corre el riesgo de perder precisión (18-19 dígitos
  superan lo que un `double` representa con exactitud) o llegar en
  notación científica.
- Una `LINEA` leída como número puede llegar con `.0` al final si no se
  formatea de vuelta a texto, lo que rompe el match exacto contra la
  plantilla aunque sea la misma línea.
- Cada parser debe usar `DataFormatter` de Apache POI (o forzar conversión
  explícita a texto según `getCellType()`) al leer la columna de
  línea/ICCID, igual que el resolver de la plantilla — no asumir que el
  archivo del operador trae el formato de columna correcto.
- **Confirmado con archivo real de Telcel:** además de `trim` +
  `DataFormatter`/lectura de texto, la normalización a texto debe remover
  comillas simples (`'`) al inicio y al final del valor — el export real
  envuelve `ICCID` y `MSISDN` en comillas literales para forzar formato
  texto. Esta limpieza aplica tanto al lector de `.xlsx` como al de
  `.csv`, para no duplicar la lógica de normalización entre ambos.
- El valor de consumo (`ConsumoMB`) sí debe leerse/parsearse como
  numérico, ya que se sumará (`groupBy` en Telcel/Alestra) o se escribirá
  directamente en la plantilla.

## Entradas necesarias para implementar esta spec
- ✅ Alestra y Telcel confirmados con archivos reales de Fase 0 (hallazgos
  incorporados arriba).
- Pendiente: 1 archivo de muestra de ATT ya consolidado, para confirmar
  que su formato es en efecto idéntico al de Telcel post-agregación.
- Pendiente: confirmar con más de un mes de datos de Alestra qué tan
  frecuentes son las líneas duplicadas, para validar que "sumar" sigue
  siendo el comportamiento esperado y no una inconsistencia real de datos
  puntual del mes revisado.
