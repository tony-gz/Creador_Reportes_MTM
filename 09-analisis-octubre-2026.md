# 09 — Análisis con datos reales: reporte de octubre 2026

> Corrida del motor de la app contra los archivos reales de octubre 2026
> (carpeta local `archivos/`, excluida de git por contener datos de
> clientes) y comparación celda por celda contra el reporte hecho a mano
> (`CONSUMOS_OCTUBRE_2026.xlsx`). Fecha: 2026-11-05. Aquí solo van hojas,
> filas y conteos — nunca líneas ni ICCID.

## Archivos analizados
| Archivo | Qué es | Resultado del parser de la app |
|---|---|---|
| Alestra `.xlsx` | Tal cual lo manda Alestra | `AlestraParser`: 637 líneas, sin avisos. Idéntico a la hoja BD_ALESTRA. |
| Telcel `.csv` | **Tabla dinámica ya preparada** (no el export crudo) | `TelcelParser` lo rechaza con mensaje claro (encabezados `Etiquetas de fila` / `Suma de USO (MB)`). Falta probar con el export crudo. |
| ATT | Hoja BD_ATT del reporte (consolidado a mano) | `AttParser`: 131 ICCID, 5 bloques lado a lado, sin avisos; suma idéntica a la del reporte. |

## Resultado de la comparación
- **1,164 celdas de consumo comparadas: 1,130 iguales al reporte manual.**
- Las 34 diferencias tienen explicación y ninguna es un error de cálculo
  del motor:

| Causa | Celdas | Qué pasa |
|---|---|---|
| SIM repetida en la plantilla | 9 | El `BUSCARV` manual repite el consumo en cada aparición (lo cuenta dos veces); la app lo asigna a la primera y avisa. Hojas: BIMBO, DENTALIA (2), TIENDAS EXTRA (6, una de ellas oculta por la F del ICCID). |
| Telcel con valor fijo escrito a mano | 21 | METROBÚS (17) y DICONSA (4): SIMs de Telcel cuyo consumo no viene en el archivo de Telcel analizado (la tabla dinámica). Hay que confirmar con el export crudo de dónde salen. |
| Consumo en ALSEA bloque 2 | 0 en la comparación, pero ver abajo | — |

## Hallazgos que afectan al diseño (Fase 2, punto 2 — lector de la plantilla)
1. **ALSEA tiene un segundo bloque fuera de toda tabla nativa** ("ALSEA 3ER
   ENLACE", encabezado en la fila 36, ~94 SIMs, filas 37-130). Sus valores
   son correctos (constantes iguales a Alestra), pero un lector que solo
   lea `getTables()` se lo saltaría **en silencio**: 118,156 MB. Decisión:
   el lector debe detectar filas de encabezado fuera de las tablas nativas
   y fallar con un error claro (misma filosofía que `HojaSinTablaException`);
   el usuario convierte el bloque en tabla nativa.
2. ~~**7 hojas siguen sin tabla nativa**~~: ASSA ABLOY, DENTALIA, BANCO
   INTERCAM, TIENDAS EXTRA, METROBÚS, DICONSA, TOTAL PLAY - OTROS. *Resuelto
   el 2026-11-09, también el bloque 2 de ALSEA (spec 10, D-017/D-018).*
3. **Las hojas BD_ATT, BD_ALESTRA y BD_TELCEL** viven dentro del reporte. El
   lector de la plantilla debe ignorar las hojas `BD_*` (sus tablas no son
   de clientes y harían fallar la clasificación).
4. **Filas que la app no puede resolver** y que hoy llevan un valor escrito
   a mano: STARLINK en METROBÚS (una cuarta fuente, sin línea ni ICCID de
   los operadores) y las SIMs de Telcel con valor fijo. Si el escritor pone
   0 en toda fila sin coincidencia, **borraría esos valores**. Pendiente de
   decidir (ver abajo).
5. **DICONSA** registra SIMs de Telcel en una tabla dual por `LINEA_2`; el
   tipo `DUAL_SIM` solo busca líneas en Alestra.

## Hallazgos de datos (para revisar en el reporte)
- **42 líneas de Alestra (193,948 MB) no aparecen en ninguna hoja** de la
  plantilla — consumo que hoy no se reporta a ningún cliente. La lista está
  en la salida del análisis (no se guarda en el repo).
- 1 ICCID de Telcel con 5.81 MB y 9 de ATT con 0 MB tampoco aparecen.
- La hoja ASSA ABLOY tiene como título "KOF" (copiado de PROPIMEX).
- Nombres de hoja distintos a la spec 01 ("KUEHENE NAGEL", "PETCO " con
  espacio, "TOTAL PLAY - OTROS"); la app no depende de los nombres.

## Cambios hechos en la app a raíz de este análisis
- `NormalizadorClave` quita la `F` de relleno de los ICCID de 19 dígitos (la
  plantilla la trae en Telcel; el export de Telcel no).
- `AttHeaderResolver` reconoce `Uso de ciclo hasta la fecha (MB)` (encabezado
  real de Jasper).
- El parser de Telcel/ATT lee **varios bloques** ICCID + consumo lado a lado
  (formato BD_ATT) y no avisa por filas vacías.

## Decisiones pendientes del usuario
> Las decisiones ya tomadas se registran en `10-registro-decisiones.md`.

1. ~~**SIMs repetidas**~~ — resuelto: entre hojas distintas se cuenta en
   todas, dentro de una hoja la primera (D-001, D-006, D-013; implementado
   el 2026-11-09). TIENDAS EXTRA 283/286: la 283 va en 0 (D-014).
2. **Filas con valor manual**: las de Telcel en METROBÚS y DICONSA salen del
   RMS de Teltonika (D-010); a futuro, API de Teltonika. El escritor pone 0
   en toda fila sin coincidencia, también en STARLINK y en los textos: el
   valor que traen es del mes pasado (D-015).
3. ~~**Export crudo de Telcel**~~ — ya no hace falta para METROBÚS/DICONSA
   (D-010). Sigue siendo útil para probar `TelcelParser` con el archivo
   real.
4. ~~Las 42 líneas de Alestra sin cliente~~ — se listan en el reporte final
   de cada corrida para que el usuario las revise (D-011). Con la plantilla
   oficial son 40.

**Corrida del 2026-11-09** (plantilla oficial = la corregida, D-012 y
D-014, y la regla nueva): 1,258 celdas comparadas, 1,227 iguales al reporte
manual. Las 31 diferencias son todas esperadas: 25 valores del RMS de
Teltonika (METROBÚS 21, DICONSA 4) y 3 textos (`DESACTIVADO`, `NA`), que la
app pone en 0 (D-015), y 3 SIMs repetidas dentro de una hoja que quedan en 0
(DENTALIA 49, TIENDAS EXTRA 270 y 273; D-006). (Esta vez se
leyeron también las tablas con encabezado `CONSUMO (MB)` con espacio: KUEHENE
NAGEL, PETCO y ANIXTER.)
