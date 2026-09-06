package com.simreports.core.matching;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de resolver una {@link TablaConFilas}: los valores a escribir
 * para cada una de sus filas, en el mismo orden en que se recibieron —
 * spec 03, "Contrato de entrada del motor".
 */
public record ResultadoTabla(String identificador, List<ResultadoFilaPlantilla> filas) {

    public ResultadoTabla {
        Objects.requireNonNull(identificador, "identificador no puede ser null");
        Objects.requireNonNull(filas, "filas no puede ser null");
        filas = List.copyOf(filas);
    }
}