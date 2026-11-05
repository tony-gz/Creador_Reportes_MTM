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
2. **7 hojas siguen sin tabla nativa**: ASSA ABLOY, DENTALIA, BANCO
   INTERCAM, TIENDAS EXTRA, METROBÚS, DICONSA, TOTAL PLAY - OTROS.
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

1. **SIMs repetidas** (9 en octubre): ¿qué fila debe llevar el consumo? Hoy
   el reporte manual lo cuenta en todas; la app, solo en la primera.
   *En curso:* D-001 (entre hojas distintas → se cuenta en las dos) y D-002
   (dentro de la misma hoja → solo la fila válida) ya registradas.
2. **Filas con valor manual** (STARLINK, Telcel fuera del archivo): ¿la app
   las deja intactas, o se agrega una fuente "manual"/Starlink?
3. **Export crudo de Telcel**: compartirlo para probar el parser real y ver
   si incluye las SIMs de METROBÚS y DICONSA.
4. Las 42 líneas de Alestra sin cliente: ¿son altas pendientes en la
   plantilla?
