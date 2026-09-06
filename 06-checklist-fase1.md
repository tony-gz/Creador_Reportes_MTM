# 06 — Checklist de progreso (Fase 1: motor de parsing puro)

> Se actualiza al cerrar cada punto. Si se retoma en otra sesión de chat,
> subir este archivo (o pegar su contenido) junto con las specs 00–05
> para saber exactamente dónde se quedó.

- [x] 1. Modelo de dominio compartido: `ResultadoParseo`, `AvisoParseo`,
      `TipoAviso`, `ArchivoFormatoInvalidoException`,
      `HojaSinTablaException`, interfaz `OrigenParser`.
      **Entregado:** 2026-09-05. `mvn test`: 7/7 OK.
- [x] 2. `HeaderResolver`: interfaz + normalización + implementación para
      la plantilla (`PlantillaHeaderResolver`).
      **Entregado:** 2026-09-05. `mvn test`: 35/35 OK.
- [x] 3. Resolver de tablas de la plantilla (clasificación
      simple/dual/cascada, spec 01).
      **Entregado:** 2026-09-06. `mvn test`: 45/45 OK.
- [x] 4. Parser de Alestra (spec 02).
      **Entregado:** 2026-09-06. `mvn test`: 59/59 OK.
- [x] 5. Parser de Telcel (`.csv` y `.xlsx`, spec 02).
      **Entregado:** 2026-09-06. `mvn test`: 74/74 OK.
- [x] 6. Parser de ATT (reutiliza lógica de Telcel post-agregación).
      **Entregado:** 2026-09-06. `mvn test`: 87/87 OK.
- [x] 7. Motor de coincidencia (spec 03): cascada, dual SIM, no
      encontrados, consistencia de totales.
      **Entregado:** 2026-09-06. `mvn test`: 106/106 OK.
- [x] 8. Interfaz `ReglaAmbigua` con implementación default.
      **Entregado:** 2026-09-06. `mvn test`: 113/113 OK.

**Fase 1 (motor de parsing puro) COMPLETA — 8/8 puntos.**

## Convención de paquete
Se usó `com.simreports.core` como paquete base para el motor de parsing
puro (Fase 1, sin Spring). Es un nombre provisional — renómbralo con
buscar/reemplazar si el proyecto ya tiene un groupId definido; no afecta
nada del diseño.

## Notas de decisiones tomadas durante la entrega (para no repetir la
discusión en otra sesión)
- Las excepciones del dominio (`ArchivoFormatoInvalidoException`,
  `HojaSinTablaException`) son **unchecked** (extienden
  `RuntimeException`), para no ensuciar la firma de `OrigenParser` ni de
  los resolvers con `throws` — se capturan explícitamente en la capa web
  en Fase 2 (`@ControllerAdvice`, spec 04).
- `AvisoParseo` cubre los dos casos que piden las specs 02: clave
  duplicada (agregada) y clave vacía/malformada (excluida). Si en Fase 1
  aparece un tercer tipo de aviso no previsto, se agrega un valor más al
  enum `TipoAviso` sin romper lo ya construido.
- `HeaderResolver.normalizar(...)` es un método estático en la interfaz
  (no una clase utilitaria aparte) porque toda implementación de
  `HeaderResolver` (plantilla y los tres parsers de origen) debe compartir
  exactamente la misma normalización — vive junto al contrato que la
  necesita.
- `PlantillaHeaderResolver` vive en el paquete `core.plantilla` (no
  directo en `core`) porque el punto 3 (resolver de tablas) va a agregar
  más clases a ese mismo paquete y conviene agruparlas desde ahora.
- `pom.xml` usa `maven.compiler.release=21` aunque el JDK instalado sea
  25 — 21 es LTS y es lo que va a usar Spring Boot en Fase 2; no limita
  nada de lo hecho en Fase 1.
- Apache POI 5.2.5 agregado al `pom.xml` en el punto 4 (scope normal, no
  test): `poi` + `poi-ooxml`, versión `${poi.version}=5.2.5`.
- Ajustes a código de Claude dictados por la API real de POI 5.2.5 (los
  fijó Claude, no opencode):
  - `AlestraParser` evita lambdas dentro del bucle de columnas (la
    variable `i` no era effectively final) → `if (campo.isPresent())`.
  - `XSSFTableColumn` en 5.2.5 **no** expone `getCTTableColumn()`:
    el nombre de columna real se fija con `setName(...)` público.
  - `Row.MissingCellPolicy` en 5.2.5 no tiene `RETURN_BLANK_CELL`; se usa
    `CREATE_NULL_AS_BLANK` para garantizar celda no-nula al leer filas
    (no modifica el archivo).
- `ParserIccidConsumoGenerico` (package-private, en `core.parsers`) es el
  motor compartido por Telcel y ATT: soporta `.csv`/`.xlsx` detectados por
  contenido (firma ZIP `PK`), busca la fila de encabezado dentro de las
  primeras 20 filas, remueve comillas simples literales alrededor de
  ICCID/MSISDN y agrega por ICCID. En Telcel **no** se genera aviso de
  clave duplicada (una fila por fecha es la forma normal del archivo);
  en Alestra sí, y además se suman los duplicados.
- `AlestraParser` lee la primera tabla nativa de `XSSFSheet.getTables()`
  en cualquier hoja del libro; si no hay tabla nativa lanza
  `ArchivoFormatoInvalidoException` (no adivina rangos).
- `ReglaAmbigua` se aísla como interfaz en `core.matching` (spec 03). La
  implementación default (`PrimeraCoincidenciaGanaReglaAmbigua`) deja la
  2ª aparición en adelante en 0 MB y genera un `AvisoParseo` de tipo
  `CLAVE_DUPLICADA` con la clave, la tabla y el consumo que habría
  correspondido. `MotorCoincidencia` tiene un constructor extra que
  recibe la regla inyectada (para tests y para cuando Fase 0 confirme el
  comportamiento real). Con la regla default, duplicar una línea en dos
  hojas ya NO dispara advertencia de consistencia (el total escrito
  cuadra); dispara aviso de duplicado.
- `ContextoFila` exige `numeroDeAparicion >= 2` y rechaza valores
  menores con `IllegalArgumentException` (la primera aparición nunca
  pasa por `ReglaAmbigua`).

## Regla para actualizar este checklist y `estructura-carpetas.txt`
Estos dos archivos son bookkeeping puro (marcar `[x]`/`[ ]`, agregar
fechas y notas), no código ni diseño — por eso **quedan fuera de la regla
dura de la spec 05** ("opencode no decide nada por su cuenta"). A partir
de ahora:
- Cuando `mvn test` pase después de un punto del checklist, opencode
  puede marcar ese punto como `[x]` en este archivo y en
  `estructura-carpetas.txt` (agregando fecha y resultado de `mvn test`,
  igual que las entradas ya existentes), **sin esperar a que Claude le
  dicte el texto exacto**.
- Esto **no** aplica a nada dentro de `src/` (código o tests): eso sigue
  viniendo siempre de Claude, tal cual, sin que opencode complete ni
  corrija nada por su cuenta.
- Si hay duda sobre si algo es "solo bookkeeping" o ya es diseño, se trata
  como diseño (se le pregunta a Claude) — la regla dura de spec 05 gana en
  caso de ambigüedad.
