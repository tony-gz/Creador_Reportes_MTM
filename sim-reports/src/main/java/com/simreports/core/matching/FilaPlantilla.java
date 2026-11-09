package com.simreports.core.matching;

/**
 * Una fila ya leída de una tabla de la plantilla, con los valores de las
 * columnas clave necesarias para el matching — spec 03.
 *
 * <p>Los valores deben venir como texto (spec 01, "Tipo de dato de celda
 * por campo lógico": nunca un {@code double} convertido a texto con
 * notación científica). El motor aplica {@link
 * com.simreports.core.NormalizadorClave} a cada clave antes de buscarla —
 * la misma limpieza que usan los parsers de origen —, así que espacios,
 * guiones, paréntesis o comillas simples alrededor no impiden el
 * match.</p>
 *
 * <p>Qué campos aplican depende del
 * {@link com.simreports.core.plantilla.TipoTablaPlantilla} de la tabla a
 * la que pertenece la fila:</p>
 * <ul>
 *   <li>{@code SIMPLE_LINEA}: solo {@link #linea()}.</li>
 *   <li>{@code SIMPLE_ICCID}: solo {@link #iccid()}.</li>
 *   <li>{@code SIMPLE_CASCADA}: {@link #linea()} e {@link #iccid()}.</li>
 *   <li>{@code DUAL_SIM}: {@link #linea1()} y {@link #linea2()} (no
 *       {@link #linea()} ni {@link #iccid()}).</li>
 * </ul>
 *
 * <p>Un campo que no aplica al tipo de tabla, o cuya celda llegó vacía en
 * Excel (equipo dado de baja — spec 00), se representa como
 * {@code null}. Una cadena vacía o solo con espacios también se acepta:
 * el motor la trata igual que {@code null} tras normalizarla.</p>
 *
 * @param sim       valor de la columna {@code SIM} / {@code Carrier} tal
 *                  cual viene en la plantilla (operador de la SIM), o
 *                  {@code null} si la tabla no tiene esa columna. Decide
 *                  dónde se busca la SIM en las tablas simples (D-016, ver
 *                  {@link OperadorPlantilla}). No aplica a {@code DUAL_SIM}.
 * @param filaExcel número de fila en la hoja de Excel (1 = primera fila),
 *                  solo para el reporte final de la corrida (p. ej. "SIM
 *                  repetida en filas 13 y 49"). No afecta el matching.
 *                  {@code null} si quien arma la fila no lo conoce; el lector
 *                  de la plantilla siempre debe llenarlo (ver
 *                  {@link #enFila(int)}).
 */
public record FilaPlantilla(String linea, String iccid, String linea1, String linea2, String sim, Integer filaExcel) {

    public FilaPlantilla {
        if (filaExcel != null && filaExcel < 1) {
            throw new IllegalArgumentException("filaExcel debe ser >= 1, fue " + filaExcel);
        }
    }

    /** Fila sin operador ni número de fila conocidos. */
    public FilaPlantilla(String linea, String iccid, String linea1, String linea2) {
        this(linea, iccid, linea1, linea2, null, null);
    }

    /** Copia de esta fila con su número de fila de Excel (1 = primera fila). */
    public FilaPlantilla enFila(int filaExcel) {
        return new FilaPlantilla(linea, iccid, linea1, linea2, sim, filaExcel);
    }

    /** Copia de esta fila con el valor de su columna {@code SIM} / {@code Carrier}. */
    public FilaPlantilla conSim(String sim) {
        return new FilaPlantilla(linea, iccid, linea1, linea2, sim, filaExcel);
    }

    public static FilaPlantilla simplePorLinea(String linea) {
        return new FilaPlantilla(linea, null, null, null);
    }

    public static FilaPlantilla simplePorIccid(String iccid) {
        return new FilaPlantilla(null, iccid, null, null);
    }

    public static FilaPlantilla cascada(String linea, String iccid) {
        return new FilaPlantilla(linea, iccid, null, null);
    }

    public static FilaPlantilla dualSim(String linea1, String linea2) {
        return new FilaPlantilla(null, null, linea1, linea2);
    }
}