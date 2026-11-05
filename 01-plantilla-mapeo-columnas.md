# 01 — Plantilla y mapeo de columnas por hoja

## Estructura de la plantilla
- Archivo `.xlsx` con **18 hojas fijas**, una por cliente, en este orden y
  con estos nombres exactos:
  `ALSEA, BIMBO, GEPP, KUEHNE NAGEL, PETCO, ANIXTER, CAPUFE, BANBAJIO,
  MEAT ME, ESTEE LAUDER, PROPIMEX(KOF), DENTALIA, BANCO INTERCAM,
  ASSA ABLOY, TIENDAS EXTRA, METROBÚS, DICONSA, TOTAL PLAY-OTROS`.
- Cada hoja tiene **1 o 2 tablas nativas de Excel** (`Table` de Excel, no
  solo rango con encabezados). Se leen con `XSSFSheet.getTables()`, lo que
  ya nos da el rango exacto y los encabezados reales — no hace falta
  "adivinar" dónde empieza la tabla.
  - **Requisito operativo (confirmado con la plantilla real de Fase 0):**
    el usuario mantiene las 18 hojas siempre con tabla nativa — al revisar
    la plantilla de agosto 2026, 7 de las 18 hojas todavía eran rango
    suelto sin `Table`. El resolver **no** implementa un modo de
    "adivinar" el rango a partir de encabezados sueltos: si `getTables()`
    devuelve vacío en alguna hoja, se lanza `HojaSinTablaException`
    señalando el nombre de la hoja, en vez de construir un rango inferido
    que podría tomar filas de más o de menos sin que nadie lo note. Es más
    seguro fallar ruidosamente una vez al mes (y que el usuario corrija la
    hoja) que arriesgar un rango mal inferido en silencio.
- Dos **tipos de tabla**:
  - **Simple**: una línea por SIM.
  - **Dual SIM (router)**: dos líneas por equipo.

> **Nota de alcance**: esta estandarización aplica solo a la **plantilla**,
> que el usuario controla y edita directamente. Los archivos de origen
> (Alestra/Telcel/ATT) llegan con el formato que cada operador/herramienta
> entrega y **no se pueden estandarizar** — ver spec 02. Sin embargo, el
> **mecanismo** de resolución de encabezados (interfaz `HeaderResolver`
> definida más abajo) sí se reutiliza en los tres parsers de origen, para
> no mantener dos filosofías distintas de manejo de encabezados en el
> mismo proyecto.

## Nomenclatura estandarizada de encabezados (decisión tomada)
Para reducir el riesgo del resolver, la plantilla se edita una sola vez para
que **todas las tablas de las 18 hojas usen los mismos encabezados**:

```
Tablas simples (15 de las 18 hojas):
  LINEA, ICCID, CONSUMO(MB)

Tablas dual SIM / router (3 de las 18 hojas):
  LINEA_1, LINEA_2, ICCID, CONSUMO(MB)_1, CONSUMO(MB)_2, CONSUMO_TOTAL
```

Notas sobre la convención:
- Excel no permite encabezados duplicados dentro de una misma `Table`, por
  eso las tablas dual SIM necesitan `LINEA_1`/`LINEA_2` y
  `CONSUMO(MB)_1`/`CONSUMO(MB)_2` en vez de repetir `LINEA`/`CONSUMO(MB)`.
- El resto de columnas de la tabla (SIM, SERIE, UNIDAD, MARCA, MODELO,
  etc.) **no necesitan estandarizarse** — el resolver las ignora, solo le
  importan los campos lógicos de arriba.
- `CONSUMO_TOTAL` en las tablas dual SIM se recalcula siempre
  (`CONSUMO(MB)_1 + CONSUMO(MB)_2`), no se lee como dato de entrada.
- Solo las 3 hojas con router necesitan editarse a la nomenclatura dual;
  las 15 hojas simples restantes no cambian.

## Tipo de dato de celda por campo lógico
- `LINEA`, `LINEA_1`, `LINEA_2`, `ICCID` → **deben leerse/tratarse como
  texto**, sin importar el formato de celda que tenga la plantilla:
  - `ICCID` tiene 18-19 dígitos, más allá de la precisión exacta de un
    `double` (~15-16 dígitos significativos); si Excel lo trae como número,
    los últimos dígitos pueden llegar redondeados o corrompidos (y a veces
    se muestra en notación científica). Como texto no hay pérdida.
  - `LINEA` no tiene el problema de precisión (10 dígitos caben exactos en
    un `double`), pero un prefijo, espacio, o cero a la izquierda se pierde
    si se lee como número, y un `double` sin formatear de vuelta a texto
    agrega un `.0` al final que rompe el matching contra Alestra.
  - El resolver debe leer estas celdas con `DataFormatter` de Apache POI
    (o forzando conversión explícita a texto según `getCellType()`) en vez
    de asumir que el usuario dejó el formato de columna correcto en las 18
    hojas — es la misma filosofía de red de seguridad barata que ya aplica
    a la normalización de encabezados.
- `CONSUMO(MB)`, `CONSUMO(MB)_1`, `CONSUMO(MB)_2` → **deben ser numéricos**,
  ya que `CONSUMO_TOTAL` se calcula sumando `CONSUMO(MB)_1 + CONSUMO(MB)_2`
  en tiempo real.

## Diseño: el mapeo sigue siendo dato, no código
Aun con nombres estandarizados, se mantiene el resolver (en vez de
hardcodear `si-hoja-es-BIMBO-entonces-columna-tal`), porque:
- Da una **red de seguridad barata**: Excel a veces introduce espacios
  invisibles, o alguien vuelve a tocar la plantilla en el futuro y no
  recuerda la convención exacta.
- Permite **corregir manualmente desde la UI** si alguna hoja queda mal
  editada, sin tocar código.

### 1. Campos lógicos y normalización (ya no diccionario de sinónimos)
Con la convención fija, el diccionario grande de variantes desaparece. Solo
queda una normalización mínima antes de comparar:
```
normalizar(header) = header.trim().toUpperCase().replaceAll("[\\s\\p{Z}\\uFEFF\\u200B]+", "")
```
**Actualizado (2026-11-05):** además de `\s`, se eliminan los espacios
invisibles que `\s` de Java no cubre: espacio de no separación (NBSP,
`U+00A0`) y demás separadores Unicode (`\p{Z}`), BOM (`U+FEFF`, típico
al inicio de un `.csv` "UTF-8 con BOM") y espacio de ancho cero
(`U+200B`).

**Actualizado con datos reales de Fase 0:** la versión original de esta
normalización solo *colapsaba* espacios múltiples a uno (`replaceAll("\\s+",
" ")`), pero 3 de las 18 hojas reales traen `"CONSUMO (MB)"` con **un solo
espacio** antes del paréntesis, que esa versión no eliminaba y por lo tanto
no matcheaba contra `"CONSUMO(MB)"`. Se cambió a remover **todos** los
espacios internos, no solo colapsarlos, para que estas variantes no
requieran agregarse una por una al diccionario.
```
LINEA         → "LINEA"
LINEA_1       → "LINEA_1"
LINEA_2       → "LINEA_2"
ICCID         → "ICCID"
CONSUMO(MB)   → "CONSUMO(MB)"
CONSUMO(MB)_1 → "CONSUMO(MB)_1"
CONSUMO(MB)_2 → "CONSUMO(MB)_2"
```
Si un encabezado normalizado no coincide con ninguno de estos, la columna
simplemente se ignora (no rompe el proceso) y queda registrada como "no
reconocida" en el mapeo persistido, visible en la UI para revisión manual.

### 2. Interfaz `HeaderResolver` (reutilizable en spec 02)
Para evitar dos implementaciones paralelas del mismo problema (mapear
encabezados reales a campos lógicos), se define una interfaz común:

```java
interface HeaderResolver {
    // Devuelve el campo lógico resuelto para un encabezado real,
    // o Optional.empty() si no se reconoce.
    Optional<String> resolver(String headerReal);
}
```

- La plantilla usa una implementación con la lista fija de campos lógicos
  de arriba (`LINEA`, `ICCID`, `CONSUMO(MB)`, etc.).
- Los parsers de Telcel/ATT (spec 02) usan otra implementación con su
  propia lista fija (`ICCID`, `USO (MB)` / `Consumo_MB`).
- Ambas comparten la misma normalización (`trim + upper + espacios`) y el
  mismo comportamiento ante un encabezado no reconocido: **no rompe el
  proceso**, se reporta explícitamente en vez de fallar en silencio o
  lanzar una excepción críptica de Apache POI.
- Esto significa que si un operador cambia el nombre de una columna en su
  export, el sistema responde igual que si la plantilla tuviera un
  encabezado raro: lo marca como "no reconocido" y lo expone al usuario,
  en vez de sumar cero silenciosamente.

### 3. Resolver de tablas de la plantilla
Para cada tabla detectada en cada hoja: normaliza sus encabezados reales
usando `HeaderResolver` y clasifica la tabla según los campos lógicos
resueltos.

Reglas de clasificación de la tabla, una vez resueltos los campos:
- Tiene `CONSUMO(MB)_1` y `CONSUMO(MB)_2` → tabla **dual SIM**: matchear
  `LINEA_1` y `LINEA_2` por separado, luego
  `CONSUMO_TOTAL = CONSUMO(MB)_1 + CONSUMO(MB)_2`.
- Tiene `LINEA` e `ICCID` a la vez (caso real de las tablas simples,
  columnas `LINEA, ICCID, CONSUMO(MB)`) → tabla **simple, matching en
  cascada por fila**: cada SIM pertenece a un solo operador, así que se
  intenta primero `LINEA` contra Alestra y, solo si no hay match, `ICCID`
  contra Telcel + ATT. Se asume que la misma línea/ICCID nunca aparece en
  dos operadores a la vez, por lo que no hace falta desambiguar con la
  columna `SIM` (que indica el operador) — queda como posible mejora futura
  (validación cruzada opcional para el reporte de inconsistencias), no
  bloquea la implementación de Fase 1.
- Tiene solo `LINEA` (sin `ICCID` ni `LINEA_2`) → tabla **simple por
  línea**: matchear contra Alestra.
- Tiene solo `ICCID` (sin columnas de línea) → tabla **simple por ICCID**:
  matchear contra Telcel + ATT.

> Nota: la spec 03 (motor de coincidencia) describe la ejecución detallada
> de la cascada fila por fila; esta sección solo define cómo el resolver
> clasifica la tabla para decidir qué estrategia de matching aplica.

### 4. Persistencia del mapeo
El resultado del resolver se guarda como parte de una **versión inmutable**
de `PlantillaConfig` (ver spec 04) para no re-detectar en cada corrida y
para dejar trazabilidad de qué columnas quedaron sin reconocer, si las hay,
y de qué versión de plantilla se usó en cada reporte generado.

## Riesgo del módulo (actualizado)
Con la nomenclatura estandarizada, este resolver deja de ser el componente
de mayor riesgo del proyecto — se vuelve un match casi exacto con
normalización de respaldo. El riesgo real del proyecto se mueve al **motor
de coincidencia** (spec 03) y a los **parsers de origen** (spec 02), que sí
dependen de formatos que el usuario no controla.

## Entradas necesarias para implementar esta spec
- ✅ Confirmado con la plantilla real de agosto 2026: nomenclatura de
  encabezados y tipos de tabla (simple / dual SIM) coinciden con lo
  documentado.
- Pendiente del usuario (compromiso tomado, no bloquea Fase 1): convertir
  las 7 hojas que aún son rango suelto a tabla nativa de Excel, ya que el
  resolver depende de `getTables()` sin fallback (ver arriba).
