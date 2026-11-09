package com.simreports.reportes;

import com.simreports.core.matching.DescuadreReporteEscrito;

/**
 * El .xlsx escrito no contiene los MB que asignó el motor
 * ({@code VerificadorReporteEscrito}): es un error del escritor, no de los
 * datos. El reporte no se guarda ni se entrega (spec 08, punto 4).
 */
public class ReporteDescuadradoException extends RuntimeException {

    public ReporteDescuadradoException(DescuadreReporteEscrito descuadre) {
        super(String.format("El reporte escrito no cuadra: se esperaban %.2f MB en las celdas de consumo y se"
                        + " leyeron %.2f MB (diferencia %.2f MB). No se guardó el reporte.",
                descuadre.totalEsperado(), descuadre.totalLeido(), descuadre.diferencia()));
    }
}
