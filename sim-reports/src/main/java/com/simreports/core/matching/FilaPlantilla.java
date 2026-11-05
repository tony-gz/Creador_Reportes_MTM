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
 */
public record FilaPlantilla(String linea, String iccid, String linea1, String linea2) {

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