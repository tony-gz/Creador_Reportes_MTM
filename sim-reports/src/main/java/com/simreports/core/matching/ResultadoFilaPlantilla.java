package com.simreports.core.matching;

import java.util.Objects;

/**
 * Valores a escribir en una fila de una tabla de la plantilla, ya
 * resueltos por el motor de coincidencia — spec 03.
 *
 * <p>Qué campos aplican depende del tipo de tabla de origen de la fila:</p>
 * <ul>
 *   <li>{@code SIMPLE_LINEA} / {@code SIMPLE_ICCID} / {@code SIMPLE_CASCADA}:
 *       solo {@link #consumo()} — escribir en la columna
 *       {@code CONSUMO(MB)}. {@link #consumo1()}, {@link #consumo2()} y
 *       {@link #consumoTotal()} son {@code null}.</li>
 *   <li>{@code DUAL_SIM}: {@link #consumo1()} → {@code CONSUMO(MB)_1},
 *       {@link #consumo2()} → {@code CONSUMO(MB)_2},
 *       {@link #consumoTotal()} → {@code CONSUMO_TOTAL} (siempre
 *       recalculado, nunca leído — spec 01/03). {@link #consumo()} es
 *       {@code null}.</li>
 * </ul>
 */
public record ResultadoFilaPlantilla(
        Asignacion consumo,
        Asignacion consumo1,
        Asignacion consumo2,
        Double consumoTotal) {

    public static ResultadoFilaPlantilla simple(Asignacion consumo) {
        Objects.requireNonNull(consumo, "consumo no puede ser null");
        return new ResultadoFilaPlantilla(consumo, null, null, null);
    }

    public static ResultadoFilaPlantilla dualSim(Asignacion consumo1, Asignacion consumo2) {
        Objects.requireNonNull(consumo1, "consumo1 no puede ser null");
        Objects.requireNonNull(consumo2, "consumo2 no puede ser null");
        double total = consumo1.consumoMb() + consumo2.consumoMb();
        return new ResultadoFilaPlantilla(null, consumo1, consumo2, total);
    }
}