package com.simreports.core.reporte;

import java.util.Objects;

/**
 * El .xlsx generado por {@link EscritorReporte}.
 *
 * @param contenido              bytes del libro.
 * @param celdasEscritas         celdas de consumo escritas.
 * @param formulasSinRecalcular  fórmulas que Apache POI no pudo recalcular
 *                               (p. ej. una función que no soporta); se
 *                               recalculan al abrir el libro en Excel.
 */
public record ReporteEscrito(byte[] contenido, int celdasEscritas, int formulasSinRecalcular) {

    public ReporteEscrito {
        Objects.requireNonNull(contenido, "contenido no puede ser null");
    }
}
