# 03 — Motor de coincidencia

## Lógica de referencia (proceso manual actual)
```
=SI.ERROR(BUSCARV([@LINEA],Table1[#Todo],2,FALSO),0)
```
Es decir: buscar la línea/ICCID en el dataset de origen; si no hay
coincidencia exacta, el consumo queda en **0** (nunca error, nunca celda
vacía).

## Reglas por tipo de tabla (según el mapeo de la spec 01)
Para cada tabla ya mapeada de la plantilla:

1. **Tabla simple con `LINEA` e `ICCID` a la vez** (caso real: columnas
   `LINEA, ICCID, CONSUMO`):
   - Cada SIM pertenece a un solo operador, así que se resuelve en
     **cascada por fila**: primero buscar `LINEA` contra **Alestra**; si
     no hay match, buscar `ICCID` contra **Telcel + ATT combinados**.
   - Insertar el MB encontrado en la primera fuente que dé match, o `0` si
     ninguna de las dos encuentra coincidencia.
   - No se usa la columna `SIM` (operador) para decidir dónde buscar —
     se asume que la misma línea/ICCID nunca aparece en dos operadores a
     la vez. Queda como mejora futura opcional: si en algún momento se
     quiere blindar contra ese caso raro, se puede usar `SIM` como
     validación posterior (no como ruteo) y marcar la discrepancia en el
     reporte de "no encontrados"/inconsistencias.

2. **Tabla simple solo por línea** (columna `LINEA`, sin `ICCID` ni
   `LINEA_2`):
   - Buscar cada valor de `LINEA` contra el dataset de **Alestra**.
   - Insertar el MB encontrado, o `0` si no hay coincidencia.

3. **Tabla simple solo por ICCID** (columna `ICCID`, sin columnas de
   línea):
   - Buscar cada valor de `ICCID` contra **Telcel + ATT combinados**.
   - Insertar el MB encontrado, o `0` si no hay coincidencia.

4. **Tabla dual SIM / router** (`LINEA_1` + `LINEA_2`, o `CONSUMO_1` +
   `CONSUMO_2`) — solo 3 de las 18 hojas:
   - Resolver `LINEA_1` contra Alestra → `CONSUMO_1`.
   - Resolver `LINEA_2` contra Alestra → `CONSUMO_2`.
   - Recalcular `CONSUMO_TOTAL = CONSUMO_1 + CONSUMO_2`.
   - Ver "Reglas ambiguas pendientes de confirmar" más abajo para el caso
     de un router con una SIM por línea y otra por ICCID.

## Coincidencias no encontradas
- Nunca deben producir error ni celda vacía: siempre `0`, igual que la
  fórmula actual.

## Reporte secundario: "no encontrados"
Casi gratis de generar una vez que existe el motor de matching: son las
claves (líneas/ICCIDs) de las BDs de origen que **no matchearon en ningún
lado** — es decir, llegan en Alestra/Telcel/ATT pero no aparecen en ninguna
hoja de la plantilla.
- Utilidad: alerta temprana de líneas nuevas sin dar de alta, o typos en la
  plantilla.
- Implementación: al terminar el matching normal (plantilla → origen), se
  recorre cada dataset de origen y se marcan las claves que nunca fueron
  consultadas/matcheadas.
- Salida: lista simple `(origen, clave, consumoMB)` que se anexa como hoja
  extra del reporte o como reporte aparte — a definir en Fase 3 (UI).

## Reporte de consistencia de totales (nuevo)
Al terminar el matching, con los mismos datos que ya están en memoria (los
3 `Map` de origen y el resultado del matching), se ejecuta una validación
matemática pura, sin costo adicional de I/O:

```
para cada fuente F en {Alestra, Telcel+ATT}:
    totalOrigen      = sum(F.values())
    totalAsignado    = sum(MB escritos en la plantilla que vinieron de F)
    totalNoEncontrado = sum(MB de claves de F que quedaron en "no encontrados")

    si |totalOrigen - (totalAsignado + totalNoEncontrado)| > margen:
        marcar advertencia de inconsistencia para F
```
- `margen` es una tolerancia mínima por redondeo de `double` (p. ej.
  `0.01` MB), no una tolerancia de negocio.
- Motivo: es la red de seguridad más barata posible contra un bug de
  matching que "pierda" MB reales de forma silenciosa — el escenario más
  costoso en este dominio, porque el cliente sí lo nota en su factura
  aunque el sistema no marque ningún error explícito.
- Esta validación se implementa como función pura y se prueba con JUnit
  igual que el resto del motor (fixtures en memoria, sin Excel real).
- **Actualizado (2026-11-05) — alcance real:** calculada dentro del motor,
  esta comprobación usa los mismos contadores que el motor actualiza al
  asignar, así que con la regla ambigua default cuadra siempre por
  construcción. No detecta claves con formato distinto (terminan en "no
  encontrados", que la UI debe mostrar de forma visible) ni errores al
  escribir el `.xlsx`. La comprobación independiente es
  `VerificadorReporteEscrito`: después de escribir el reporte se relee el
  archivo, se suman las columnas de consumo y se compara contra lo que el
  motor asignó (ver checklist 08, punto 4).

## Reglas ambiguas pendientes de confirmar (interfaz `ReglaAmbigua`)
Dos reglas del motor dependen de datos reales que aún no se han
confirmado (spec 00, pendientes de Fase 0):
- Si una línea/ICCID aparece duplicada dentro de la misma hoja de la
  plantilla, o en dos hojas distintas.
- Si algún router mixto resuelve una SIM por línea y la otra por ICCID
  (hoy la spec asume que ambas SIMs de un router siempre se resuelven por
  línea).

En vez de dejar esta incertidumbre dispersa dentro del motor de matching,
se aísla detrás de una interfaz pequeña:

```java
interface ReglaAmbigua {
    ResolucionAmbigua resolver(ContextoFila fila);
}
```

- Se implementa una versión **default** razonable desde ya (p. ej.: la
  primera coincidencia gana y el resto se marca como advertencia), lo que
  permite seguir avanzando con Fase 1 sin bloquearse esperando los
  archivos de Fase 0.
- Cuando lleguen los datos reales y se confirme el comportamiento
  esperado, el cambio se limita a **esta clase pequeña**, sin tocar el
  resto del motor de coincidencia — que ya se probó y se da por correcto.
- Esto evita la alternativa de escribir la regla "a medias" dentro del
  motor y tener que reescribir lógica central del matching más adelante.

## Contrato de entrada del motor
El motor no debe saber nada de Excel ni de Spring — recibe:
- El mapeo ya resuelto de cada tabla de la plantilla (spec 01).
- Los 3 `ResultadoParseo` ya parseados (spec 02), cada uno con su `Map` de
  consumos y su lista de avisos.
Y devuelve:
- Los valores a escribir por celda/tabla de la plantilla.
- La lista de "no encontrados".
- La lista de advertencias de consistencia de totales.
- Los avisos de parseo heredados de spec 02 (duplicados, claves
  malformadas), para que la UI los muestre todos juntos.

Esto permite testear el motor con `JUnit` puro, sin tocar archivos reales,
usando los mapas de entrada como fixtures.

## Entradas necesarias para implementar esta spec
- Confirmar con datos reales de Fase 0 si existe algún caso de router mixto
  (una SIM por línea, otra por ICCID) para ajustar la implementación
  default de `ReglaAmbigua`.
