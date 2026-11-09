package com.simreports.reportes;

import com.simreports.core.matching.ResultadoMatching;

import java.util.Objects;

/** Lo que devuelve una corrida: el registro guardado y el resultado completo del motor. */
public record ReporteResultado(ReporteGenerado reporte, ResultadoMatching resultado) {

    public ReporteResultado {
        Objects.requireNonNull(reporte, "reporte no puede ser null");
        Objects.requireNonNull(resultado, "resultado no puede ser null");
    }
}
