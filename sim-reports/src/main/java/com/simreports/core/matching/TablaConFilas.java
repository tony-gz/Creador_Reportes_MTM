package com.simreports.core.matching;

import com.simreports.core.plantilla.TipoTablaPlantilla;

import java.util.List;
import java.util.Objects;

/**
 * Una tabla de la plantilla ya resuelta (spec 01) junto con las filas de
 * datos leídas de esa tabla en el Excel. Es la unidad de entrada que
 * consume el motor de coincidencia por cada una de las tablas de las 18
 * hojas — spec 03, "Contrato de entrada del motor".
 *
 * @param identificador nombre descriptivo de la tabla, solo para
 *                       trazabilidad en reportes (p. ej.
 *                       {@code "BIMBO - Table1"}). No se usa para
 *                       ninguna decisión de matching.
 * @param tipo           tipo ya clasificado por
 *                       {@link com.simreports.core.plantilla.ResolverTablaPlantilla}.
 * @param filas          filas de la tabla, en el orden en que aparecen
 *                       en Excel (el orden no afecta el resultado del
 *                       matching, solo se preserva para que el resultado
 *                       de {@link MotorCoincidencia#resolverTabla} se
 *                       pueda mapear de vuelta a la fila correcta al
 *                       escribir en el Excel).
 */
public record TablaConFilas(String identificador, TipoTablaPlantilla tipo, List<FilaPlantilla> filas) {

    public TablaConFilas {
        Objects.requireNonNull(identificador, "identificador no puede ser null");
        Objects.requireNonNull(tipo, "tipo no puede ser null");
        Objects.requireNonNull(filas, "filas no puede ser null");
        filas = List.copyOf(filas);
    }
}