package com.simreports.core.matching;

import java.util.Objects;

/**
 * Una aparición de una SIM repetida en la plantilla, para el reporte final
 * de la corrida (spec 10, D-006).
 *
 * @param hoja             hoja de Excel.
 * @param tabla            identificador de la tabla.
 * @param filaExcel        fila de Excel, o {@code null} si no se conoce.
 * @param campo            columna donde estaba la clave ({@code LINEA},
 *                         {@code ICCID}, {@code LINEA_1} o {@code LINEA_2}).
 * @param consumoAsignado  MB que el motor escribió en esa fila para esta
 *                         clave (0 si la regla la dejó en cero).
 */
public record AparicionSim(String hoja, String tabla, Integer filaExcel, String campo, double consumoAsignado) {

    public AparicionSim {
        Objects.requireNonNull(hoja, "hoja no puede ser null");
        Objects.requireNonNull(tabla, "tabla no puede ser null");
        Objects.requireNonNull(campo, "campo no puede ser null");
    }
}
