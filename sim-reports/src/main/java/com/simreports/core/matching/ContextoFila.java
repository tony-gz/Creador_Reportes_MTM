package com.simreports.core.matching;

import java.util.Objects;

/**
 * Contexto que {@link MotorCoincidencia} le pasa a una
 * {@link ReglaAmbigua} cuando una clave (línea o ICCID) se consulta por
 * segunda vez o más durante la misma corrida — spec 03, "Reglas
 * ambiguas", y decisiones D-001 / D-006 (spec 10).
 *
 * @param clave                 línea o ICCID que se está resolviendo de nuevo.
 * @param fuente                fuente contra la que matcheó esta clave
 *                              (Alestra o Telcel+ATT).
 * @param consumoMb             MB reportados por esa fuente para esta clave
 *                              (el mismo en cada aparición).
 * @param hoja                  hoja de Excel de esta aparición.
 * @param tablaIdentificador    tabla de esta aparición, para mensajes.
 * @param filaExcel             fila de Excel de esta aparición, o
 *                              {@code null} si no se conoce.
 * @param numeroDeAparicion     cuántas veces (contando esta) se ha
 *                              consultado esta clave en la corrida, sumando
 *                              todas las hojas. El motor solo invoca la
 *                              regla a partir de 2.
 * @param yaAparecioEnEstaHoja  {@code true} si la clave ya se había
 *                              consultado antes <b>en la misma hoja</b>;
 *                              {@code false} si solo había aparecido en
 *                              otras hojas.
 */
public record ContextoFila(
        String clave,
        FuenteConsumo fuente,
        double consumoMb,
        String hoja,
        String tablaIdentificador,
        Integer filaExcel,
        int numeroDeAparicion,
        boolean yaAparecioEnEstaHoja) {

    public ContextoFila {
        Objects.requireNonNull(clave, "clave no puede ser null");
        Objects.requireNonNull(fuente, "fuente no puede ser null");
        Objects.requireNonNull(hoja, "hoja no puede ser null");
        Objects.requireNonNull(tablaIdentificador, "tablaIdentificador no puede ser null");
        if (numeroDeAparicion < 2) {
            throw new IllegalArgumentException(
                    "numeroDeAparicion debe ser >= 2; la primera aparición no pasa por ReglaAmbigua");
        }
    }

    /** "hoja fila N" o solo la hoja si no se conoce la fila; para mensajes. */
    public String ubicacion() {
        return filaExcel == null ? "'" + hoja + "'" : "'" + hoja + "' fila " + filaExcel;
    }
}
