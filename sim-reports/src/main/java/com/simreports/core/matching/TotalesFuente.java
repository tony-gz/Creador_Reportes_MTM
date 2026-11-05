package com.simreports.core.matching;

import java.util.Objects;

/**
 * Totales de una fuente al cerrar el matching — spec 03, "Reporte de
 * consistencia de totales". Siempre se calculan (haya o no advertencia),
 * para que la UI pueda mostrar el resumen por fuente y para que la capa que
 * escribe el .xlsx pueda verificar lo escrito con
 * {@link VerificadorReporteEscrito}.
 *
 * @param totalOrigen       suma de todos los MB del dataset de origen.
 * @param totalAsignado     suma de los MB que el motor asignó a filas de la
 *                          plantilla desde esta fuente.
 * @param totalNoEncontrado suma de los MB de claves de esta fuente que no
 *                          aparecieron en ninguna tabla de la plantilla.
 */
public record TotalesFuente(
        FuenteConsumo fuente,
        double totalOrigen,
        double totalAsignado,
        double totalNoEncontrado) {

    public TotalesFuente {
        Objects.requireNonNull(fuente, "fuente no puede ser null");
    }
}
