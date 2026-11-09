package com.simreports.core.matching;

/**
 * El total de MB leído de vuelta del .xlsx escrito no coincide con lo que el
 * motor de coincidencia asignó — ver {@link VerificadorReporteEscrito}.
 *
 * @param totalEsperado suma de MB asignados por el motor (todas las
 *                      fuentes).
 * @param totalLeido    suma de las celdas de consumo leídas del archivo
 *                      ya escrito.
 */
public record DescuadreReporteEscrito(double totalEsperado, double totalLeido) {

    /** {@code totalEsperado - totalLeido}: positivo = faltan MB en el archivo. */
    public double diferencia() {
        return totalEsperado - totalLeido;
    }
}
