package com.simreports.reportes;

/** No existe una corrida con ese id. */
public class ReporteNoEncontradoException extends RuntimeException {

    public ReporteNoEncontradoException(Long id) {
        super("No existe el reporte " + id + ".");
    }
}
