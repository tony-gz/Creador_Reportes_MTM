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
 *                          Incluye los MB repetidos a propósito entre
 *                          hojas: es lo que debe contener el .xlsx escrito.
 * @param totalNoEncontrado suma de los MB de claves de esta fuente que no
 *                          aparecieron en ninguna tabla de la plantilla.
 * @param totalRepetidoEntreHojas parte de {@code totalAsignado} que viene de
 *                          SIMs contadas en más de una hoja (D-001): cada
 *                          hoja después de la primera vuelve a sumar el
 *                          consumo. La consistencia cuadra cuando
 *                          {@code totalOrigen = totalAsignado -
 *                          totalRepetidoEntreHojas + totalNoEncontrado}.
 */
public record TotalesFuente(
        FuenteConsumo fuente,
        double totalOrigen,
        double totalAsignado,
        double totalNoEncontrado,
        double totalRepetidoEntreHojas) {

    public TotalesFuente {
        Objects.requireNonNull(fuente, "fuente no puede ser null");
    }

    /** Totales de una corrida sin SIMs repetidas entre hojas. */
    public TotalesFuente(FuenteConsumo fuente, double totalOrigen, double totalAsignado, double totalNoEncontrado) {
        this(fuente, totalOrigen, totalAsignado, totalNoEncontrado, 0.0);
    }
}
