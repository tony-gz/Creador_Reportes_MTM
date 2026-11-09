# 10 — Registro de decisiones del usuario

> Decisiones de negocio tomadas por el usuario a partir de los hallazgos con
> datos reales (spec 09). Una entrada por decisión, con fecha; no se edita
> una decisión ya registrada: si cambia, se agrega una nueva que la
> reemplace y se marca la anterior como "Reemplazada por D-xxx".
>
> Las referencias a filas son del reporte de octubre 2026
> (`CONSUMOS_OCTUBRE_2026.xlsx`, carpeta local `archivos/`).
>
> **Identificadores:** este repositorio es público, así que aquí van solo
> hoja, fila y MB, igual que en la spec 09. Las líneas, ICCID, series y
> nombres de unidad de cada caso, necesarios para aplicar y auditar la
> decisión, están en la copia local
> `archivos/10-registro-decisiones-identificadores.md` (gitignored), con los
> mismos números D-xxx. Una decisión nueva se registra en los dos archivos.

---

## D-001 — Una SIM repetida en hojas distintas se cuenta en las dos

- **Fecha:** 2026-11-05
- **Caso:** la misma SIM (misma línea, mismo ICCID) aparece en
  **ALSEA fila 18** (SIM "Multicarrier") y en **BIMBO fila 12** (como
  `LINEA_2` de un router). Consumo de octubre: 38,809.50 MB en cada hoja.
- **Decisión:** se deja así; el consumo se reporta **en las dos hojas**.
- **Motivo:** la plantilla lleva años así y cada hoja se entrega por
  separado a su destinatario, no como un total conjunto. No se sabe por qué
  está en las dos, pero no se cambia.
- **Efecto en la app:**
  - Duplicado **entre hojas distintas** → cada aparición recibe el consumo
    completo, y se genera un aviso informativo (no un error).
  - La regla ambigua default actual (`PrimeraCoincidenciaGanaReglaAmbigua`:
    la primera aparición gana y las demás quedan en 0) **no** cumple esta
    decisión para este caso; hay que ajustarla (ver "Pendiente").
  - La consistencia de totales debe descontar esos MB repetidos a propósito
    para no marcar una inconsistencia falsa: el total escrito supera al
    total de origen exactamente por lo duplicado entre hojas.

## D-002 — TIENDAS EXTRA, SIM en filas 120 y 271: solo cuenta la fila 271

- **Fecha:** 2026-11-05
- **Caso:** la misma SIM (misma línea, mismo ICCID) aparece dos veces en la
  **misma hoja**:
  - **fila 120** — router dado de baja, modelo `NA`;
  - **fila 271** — router en funcionamiento (Extra_100).
- **Decisión:** el consumo (4,906.18 MB en octubre) va **solo en la fila
  271**. La **fila 120 va en 0**.
- **Motivo:** el usuario confirmó que hoy el número lo usa el router de la
  fila 271. Antes lo usaba el de la fila 120, que ya no está en
  funcionamiento (por eso su modelo dice `NA`).
- **Efecto en la app:**
  - Duplicado **dentro de la misma hoja** → nunca se cuenta dos veces.
  - En este caso la fila válida es la **segunda** aparición, así que "la
    primera gana" asignaría el consumo a la fila equivocada. La app no
    puede adivinar cuál es la fila válida.
  - Forma recomendada de aplicarlo: corregir la plantilla poniendo `NA` en
    la `LINEA` de la fila 120, que es la misma convención que ya usa la
    plantilla para líneas sin uso (p. ej. TOTAL PLAY - OTROS). Así la app y
    el `BUSCARV` manual dan 0 en esa fila sin reglas especiales.

## D-003 — DENTALIA, SIM repetida en filas 13 y 49: cuenta la primera

- **Fecha:** 2026-11-05
- **Caso:** una SIM de ATT aparece en **fila 13** y **fila 49** de la misma
  hoja, con la misma unidad. 216.75 MB en octubre.
- **Decisión:** se queda la **primera (fila 13)**; la fila 49 es un registro
  completamente duplicado y va en **0**.

## D-004 — TIENDAS EXTRA, SIM repetida en filas 85 y 273: cuenta la primera

- **Fecha:** 2026-11-05
- **Caso:** misma línea y mismo ICCID, misma unidad, en **fila 85** y
  **fila 273**. 169.15 MB en octubre.
- **Decisión:** igual que D-003: **fila 85** con el consumo, **fila 273 en 0**.

## D-005 — TIENDAS EXTRA, SIM repetida en filas 125 y 270: cuenta la primera

- **Fecha:** 2026-11-05
- **Caso:** misma línea y mismo ICCID, misma unidad, en **fila 125** y
  **fila 270**. 158.40 MB en octubre.
- **Decisión:** igual que D-003: **fila 125** con el consumo, **fila 270 en 0**.

## D-006 — Regla general: SIM repetida en la misma hoja = la primera gana + reporte final

- **Fecha:** 2026-11-05
- **Decisión:** "en todos los casos así": si una SIM aparece más de una vez
  en la **misma hoja**, la **primera aparición** lleva el consumo y las
  demás van en **0**. La app debe generar una **notificación tipo reporte
  al final** de la corrida, que liste cada SIM repetida: hoja, filas, cuál
  se quedó con el consumo y cuáles quedaron en 0.
- **Relación con D-001:** si la repetición es entre **hojas distintas**, se
  cuenta en todas (D-001); también debe aparecer en el reporte final, como
  informativo.
- **Relación con D-002:** cuando la fila válida no es la primera, la regla
  elegiría la fila equivocada; esos casos se corrigen en la plantilla (`NA`
  en la LINEA de la fila obsoleta), y ya no son duplicados.

## D-007 — TIENDAS EXTRA fila 258 (Extra_71): número de línea equivocado

- **Fecha:** 2026-11-05
- **Caso:** la misma línea aparece en **fila 80** y en **fila 258**
  (Extra_71), con ICCID distinto.
- **Decisión:** el número de **la fila 258 está mal**; el usuario dio el
  correcto (en la copia local). La fila 80 conserva su número.
- **Efecto:** corrección de datos en la plantilla (columna LINEA de la fila
  258). Con el número correcto, Extra_71 recupera **12,359.38 MB** de
  octubre que hoy estaban en "consumo sin cliente", y ya no hay duplicado.

## D-008 — TIENDAS EXTRA fila 139: número de línea equivocado — *Reemplazada por D-009*

- **Fecha:** 2026-11-05
- **Caso:** la misma línea aparece en **fila 139** y en **fila 255**
  (Extra_74), con ICCID distinto.
- **Decisión:** el número de **la fila 139 está mal**.
- **Pendiente de aclarar:** el número que indicó el usuario era el mismo
  que ya tienen las dos filas. No se aplicó ninguna corrección hasta
  confirmarlo (ver D-009).

## D-009 — TIENDAS EXTRA fila 255 (Extra_74): número de línea equivocado (reemplaza D-008)

- **Fecha:** 2026-11-05
- **Caso:** el mismo de D-008: una línea en **fila 139** y en **fila 255**
  (Extra_74).
- **Decisión:** **la fila 139 está bien** y conserva su número. El número
  equivocado es el de **la fila 255**; el usuario dio el correcto (en la
  copia local).
- **Efecto:** corrección de datos en la plantilla (columna LINEA de la fila
  255). Con el número correcto, Extra_74 recupera **155.12 MB** de octubre
  que hoy estaban en "consumo sin cliente", y ya no hay duplicado.

## D-010 — Telcel en METROBÚS y DICONSA: consumo aproximado del RMS de Teltonika

> *El efecto en el escritor ("no debe borrarlos") quedó reemplazado por
> D-015: esas filas van en 0. El resto de D-010 sigue vigente.*

- **Fecha:** 2026-11-09
- **Caso:** 21 filas de **METROBÚS** y 4 de **DICONSA** (`LINEA_2`, filas
  199-202) con SIMs de Telcel cuyo consumo no viene en ningún archivo de
  operador. En octubre llevan un valor escrito a mano (spec 09).
- **Decisión:** esos valores los saca el usuario del **RMS de Teltonika**
  (consumo aproximado en MB que reporta el router). No hay BD del operador
  para esas SIMs.
- **Efecto en la app:**
  - La app no puede calcularlos. El escritor del reporte (Fase 2, punto 4)
    **no debe borrarlos** al poner 0 en las filas sin coincidencia (ver la
    propuesta en spec 08, punto 4).
  - **Tarea futura:** integrar la **API de Teltonika RMS** para consultar ese
    consumo automáticamente (spec 08, "Tareas futuras").
- **Sigue pendiente:** STARLINK en METROBÚS (también valor manual, sin
  línea ni ICCID de operador).

## D-011 — Consumo sin cliente: lista en el reporte final de la app

- **Fecha:** 2026-11-09
- **Caso:** claves con consumo en las fuentes que no están en ninguna hoja de
  la plantilla. En octubre: 42 líneas de Alestra; con la plantilla corregida
  (D-012) quedan **40 líneas, 181,433.84 MB**, porque D-007 y D-009
  recuperaron 2. Además, 1 ICCID de Telcel (5.81 MB) y 9 de ATT (0 MB).
- **Decisión:** el usuario quiere verlas **como lista en el reporte final**
  de cada corrida para revisar cuáles son.
- **Efecto en la app:** sección 4 de `ReporteFinalCorrida` ("Consumo sin
  cliente"): por fuente, de mayor a menor consumo, con conteo y total.

## D-012 — La plantilla corregida es la plantilla oficial

- **Fecha:** 2026-11-09
- **Decisión:** `archivos/CONSUMOS_OCTUBRE_2026_corregido.xlsx` (correcciones
  D-002, D-007 y D-009) se adopta como la plantilla oficial.

## D-013 — Resto de SIMs repetidas: se aplica la regla general

- **Fecha:** 2026-11-09
- **Decisión:** "si están en hojas diferentes guardarlas, pero si están en la
  misma hoja, mantener el primero (excepto las que ya revisé)". Los casos ya
  revisados están corregidos en la plantilla oficial (D-002, D-007, D-009) o
  coinciden con la regla (D-003 a D-005). Los demás siguen D-001 / D-006 sin
  corrección.
- **Implementado** el 2026-11-09 (`PrimeraEnLaHojaGanaReglaAmbigua`, reporte
  final `ReporteFinalCorrida`). Corrida de octubre con la plantilla oficial:
  5 SIMs repetidas dentro de una hoja, 1 entre hojas (ALSEA fila 18 / BIMBO
  fila 12), consistencia de totales cuadrada.
- **Caso a confirmar — TIENDAS EXTRA filas 283 y 286** (10,128.70 MB) — *resuelto en D-014*:
  - **fila 283** — unidad marcada como cambio, LINEA "pendiente", ICCID con
    la F de relleno;
  - **fila 286** — otra unidad, el mismo ICCID sin la F.
  En el reporte manual de octubre el `BUSCARV` no encontró la fila 283 (por
  la F) y el consumo quedó en la **286**. La app quita la F, así que la 283
  es la primera aparición y **la app le asigna el consumo a la 283**. Si la
  fila válida es la 286, se corrige la plantilla (`NA` en el ICCID o en la
  LINEA de la 283, como D-002).

## D-014 — TIENDAS EXTRA fila 283 ("pendiente") va en 0; el consumo es de la fila 286

- **Fecha:** 2026-11-09
- **Caso:** el de D-013: el mismo ICCID en la **fila 283** (LINEA
  "pendiente") y en la **fila 286**.
- **Decisión:** la fila que dice "pendiente" (283) va en **0**; la **286**
  conserva el consumo (10,128.70 MB en octubre), como en el reporte manual.
- **Efecto:** corrección en la plantilla oficial: el **ICCID** de la fila 283
  pasa de el ICCID (con la F de relleno) a `NA`. La LINEA ya decía "pendiente", así que la
  cascada buscaba por ICCID; con `NA` ya no coincide y ya no es duplicado.

## D-015 — Filas sin coincidencia van en 0, aunque traigan un valor escrito a mano

> *La parte de STARLINK quedó reemplazada por D-017: ese bloque no está en
> ninguna tabla y la app no lo toca.*

- **Fecha:** 2026-11-09
- **Caso:** filas que la app no encuentra en ningún archivo de operador y que
  en la plantilla traen un valor fijo: los aproximados del RMS de Teltonika
  (METROBÚS, DICONSA; D-010), STARLINK, y textos como `DESACTIVADO` o `NA`.
- **Decisión:** el escritor del reporte pone **0** en todas esas filas.
  **No** conserva el valor anterior.
- **Motivo:** el valor que trae la plantilla es **el del mes pasado**;
  conservarlo reportaría un consumo viejo como si fuera del mes actual.
- **Efecto en la app:** regla única para el escritor (Fase 2, punto 4): toda
  celda de consumo se escribe con lo que da el motor (0 sin coincidencia).
  El verificador del reporte escrito no necesita casos especiales. Los
  valores del RMS los captura el usuario después, hasta que exista la API
  de Teltonika (D-010).

## D-016 — La columna SIM / Carrier decide dónde se busca la SIM

- **Fecha:** 2026-11-09
- **Caso:** la app buscaba en cascada (línea en Alestra y, si no estaba,
  ICCID en Telcel + ATT) sin mirar el operador de la fila. Una fila de
  Alestra con un ICCID de Telcel por error se llevaba consumo de Telcel.
- **Decisión:** usar la columna `SIM` (o `Carrier`, en TIENDAS EXTRA):
  - **ALESTRA** → solo por **número de línea**;
  - **MULTICARRIER** → también es Alestra: solo por línea;
  - **TELCEL** y **ATT** → solo por **ICCID**.
- **Efecto en la app** (implementado el 2026-11-09, `OperadorPlantilla`):
  - Variantes reales aceptadas: `Alestra`, `Multicarrier`, `Telcel`, `AT&T`,
    `AT&T_2`, `ATT001`, `ATT002`, `ATT02` (se comparan solo las letras).
  - Si la fila dice un operador y la SIM solo está en el otro, queda en **0**
    y sale un aviso `CARRIER_NO_COINCIDE` en el reporte final.
  - Columna vacía u otro valor (`STARLINK`, `OUI`, `Proveedor de internet`)
    → se busca como antes (cascada). Las tablas dual SIM no cambian.
  - En octubre, con la plantilla oficial, ninguna fila tiene el operador en
    desacuerdo con donde está la SIM; el resultado no cambia.

## D-017 — STARLINK de METROBÚS queda fuera de las tablas; la app no lo toca

- **Fecha:** 2026-11-09
- **Caso:** al convertir METROBÚS en tabla nativa, la hoja tiene dos bloques:
  las SIMs (filas 8-60, Telcel/Multicarrier) y "SERVICIOS STARLINK" (filas
  66-100: proveedor, unidad, serie, kit y consumo; sin línea ni ICCID y con
  celdas combinadas B:C, que una tabla de Excel no admite).
- **Decisión:** solo las SIMs se convierten en tabla. El bloque STARLINK se
  queda como está y **la app no lo lee ni lo escribe**: el usuario lo llena
  cada mes.
- **Relación con D-015:** D-015 decía que STARLINK iría en 0; esa parte
  queda reemplazada por esta decisión. El resto de D-015 sigue vigente (las
  filas de las tablas sin coincidencia van en 0).

## D-018 — BANCO INTERCAM: la segunda columna "Marca" se llama "Modelo"

- **Fecha:** 2026-11-09
- **Caso:** BANCO INTERCAM tenía dos encabezados "Marca" (G6 y H6); una tabla
  de Excel no admite nombres repetidos. La columna H trae modelos (RUT950).
- **Decisión:** H6 pasa a **"Modelo"**. No afecta el cálculo.

---

## Regla general (confirmada el 2026-11-05)

| Caso | Comportamiento | Decisión |
|---|---|---|
| Misma SIM en **hojas distintas** | Se cuenta en todas las hojas. | D-001 |
| Misma SIM **dos o más veces en la misma hoja** | La **primera** aparición lleva el consumo; las demás van en 0. | D-003 a D-006 |
| La fila válida no es la primera | Se corrige la plantilla (`NA` en la LINEA de la fila obsoleta). | D-002 |
| Número de línea equivocado en la plantilla | Se corrige la plantilla; la app no adivina. | D-007, D-009 |
| Cualquier SIM repetida | Aparece en el **reporte final de la corrida**: hoja, filas, quién se quedó el consumo y quién quedó en 0. | D-006 |

**Efecto en la app (implementado el 2026-11-09, ver D-013):**
- `ContextoFila` recibe la **hoja** de cada aparición y si la clave ya había
  aparecido en esa hoja; la regla default es `PrimeraEnLaHojaGanaReglaAmbigua`.
- La consistencia de totales descuenta los MB repetidos a propósito entre
  hojas (`TotalesFuente.totalRepetidoEntreHojas`).
- Reporte final de la corrida: `ReporteFinalCorrida` (repetidas, consumo sin
  cliente, totales). Falta mostrarlo en la UI y en el `.xlsx` de salida
  (Fase 2, punto 4).

## Correcciones en la plantilla

Aplicadas el 2026-11-05 en una **copia**:
`archivos/CONSUMOS_OCTUBRE_2026_corregido.xlsx`. El original
(`CONSUMOS_OCTUBRE_2026.xlsx`) no se modificó.

- [x] TIENDAS EXTRA fila 120: LINEA → `NA`; consumo 4,906.18 → 0 (D-002).
- [x] TIENDAS EXTRA fila 258 (Extra_71): LINEA corregida; consumo
      155.49 → 12,359.38 (D-007).
- [x] TIENDAS EXTRA fila 255 (Extra_74): LINEA corregida; consumo
      147.79 → 155.12 (D-009). La fila 139 no se tocó.

Verificación: de 17,514 celdas en las 21 hojas, cambiaron exactamente 6 (las
3 líneas y el resultado de sus 3 `BUSCARV`). Fórmulas, estilos, tablas y las
71 partes internas del archivo quedaron intactos. LibreOffice, recalculando
desde cero, llega a los mismos valores. El libro queda marcado para que
Excel recalcule al abrirlo.

- [x] El usuario adopta la copia corregida como su plantilla (2026-11-09, D-012).
- [x] TIENDAS EXTRA fila 283: ICCID → `NA` (2026-11-09, D-014), en la
      plantilla oficial. De 17,514 celdas cambió solo esa; el consumo de la
      fila ya era 0 y la 286 conserva 10,128.70 MB. Respaldo previo:
      `archivos/CONSUMOS_OCTUBRE_2026_corregido.respaldo-antes-D014.xlsx`.
- [x] Tablas nativas nuevas (2026-11-09), en el mismo formato que las 11
      existentes (sin estilo ni filtros):
  - ALSEA bloque 2 → `Tabla18`, A36:I130;
  - ASSA ABLOY → `Tabla19`, A6:H8;
  - DENTALIA → `Tabla20`, A6:H71;
  - BANCO INTERCAM → `Tabla21`, A6:I21 (H6 "Marca" → "Modelo", D-018);
  - TIENDAS EXTRA → `Tabla22`, A6:H378;
  - METROBÚS → `Tabla23`, A8:G60 (STARLINK queda fuera, D-017);
  - DICONSA → `Tabla24`, A7:J202;
  - TOTAL PLAY - OTROS → `Tabla25`, A3:I10.

  De 17,514 celdas cambió solo H6 de BANCO INTERCAM. La app acepta la
  plantilla (19 tablas, 1,020 filas) y da el mismo reporte final. LibreOffice
  la abre y reconoce las tablas. Respaldo previo:
  `archivos/CONSUMOS_OCTUBRE_2026_corregido.respaldo-antes-tablas.xlsx`.
- Las filas 49 de DENTALIA y 270 / 273 de TIENDAS EXTRA no necesitan
  corrección: la regla D-006 las deja en 0 y las reporta. Opcionalmente se
  pueden borrar de la plantilla.

## Pendiente

- [x] El usuario revisa los casos restantes de SIMs repetidas: se aplica la
      regla general (D-013).
- [x] TIENDAS EXTRA filas 283 / 286: la 283 va en 0 (D-014).
- [x] Implementar la regla general y el reporte final de duplicados
      (2026-11-09, D-013).
- [x] Origen de los consumos de Telcel de METROBÚS y DICONSA: RMS de
      Teltonika (D-010).
- [x] Claves sin cliente: se listan en el reporte final (D-011).
- [x] Valores escritos a mano (RMS, STARLINK, `DESACTIVADO`, `NA`): el
      escritor pone 0; son del mes pasado (D-015).
- [x] TIENDAS EXTRA fila 283: el usuario confirmó que sí es Telcel; el
      Carrier se queda en `TELCEL` y la fila queda en 0 por el `NA` del
      ICCID (D-014).
