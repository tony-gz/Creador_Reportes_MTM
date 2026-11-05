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

---

## Regla general (confirmada el 2026-11-05)

| Caso | Comportamiento | Decisión |
|---|---|---|
| Misma SIM en **hojas distintas** | Se cuenta en todas las hojas. | D-001 |
| Misma SIM **dos o más veces en la misma hoja** | La **primera** aparición lleva el consumo; las demás van en 0. | D-003 a D-006 |
| La fila válida no es la primera | Se corrige la plantilla (`NA` en la LINEA de la fila obsoleta). | D-002 |
| Número de línea equivocado en la plantilla | Se corrige la plantilla; la app no adivina. | D-007, D-009 |
| Cualquier SIM repetida | Aparece en el **reporte final de la corrida**: hoja, filas, quién se quedó el consumo y quién quedó en 0. | D-006 |

**Efecto en la app (por implementar):**
- `ReglaAmbigua` debe distinguir la **hoja** de cada aparición: hoy
  `ContextoFila` solo recibe un identificador de tabla, y la regla default
  pone en 0 toda repetición, también las de hojas distintas (contradice
  D-001).
- La consistencia de totales debe descontar los MB repetidos a propósito
  entre hojas.
- Reporte final de duplicados (hoja o sección en la salida y en la UI).

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

- [ ] El usuario adopta la copia corregida como su plantilla.
- Las filas 49 de DENTALIA y 270 / 273 de TIENDAS EXTRA no necesitan
  corrección: la regla D-006 las deja en 0 y las reporta. Opcionalmente se
  pueden borrar de la plantilla.

## Pendiente

- [ ] El usuario revisa los casos restantes de SIMs repetidas: TIENDAS EXTRA
      (filas 283/286, 222/361), DENTALIA (16/43, 25/26), DICONSA
      (99-107/199-202, 181/190) y ALSEA (21/22, 23/123).
- [ ] Implementar la regla general y el reporte final de duplicados.
- [ ] Resto de decisiones abiertas de la spec 09: filas con valor manual
      (STARLINK y SIMs de Telcel fuera del export), origen de los consumos
      de Telcel de METROBÚS/DICONSA, y las 52 claves sin cliente.
