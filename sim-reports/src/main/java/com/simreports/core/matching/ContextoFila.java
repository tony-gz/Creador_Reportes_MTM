package com.simreports.core.matching;

import java.util.Objects;

/**
 * Contexto que {@link MotorCoincidencia} le pasa a una
 * {@link ReglaAmbigua} cuando una clave (línea o ICCID) se consulta por
 * segunda vez o más durante la misma corrida — spec 03, "Reglas
 * ambiguas pendientes de confirmar".
 *
 * @param clave              línea o ICCID que se está resolviendo de nuevo.
 * @param fuente             fuente contra la que matcheó esta clave
 *                           (Alestra o Telcel+ATT).
 * @param consumoMb          MB reportados por esa fuente para esta clave
 *                           (el mismo en cada aparición: es la misma
 *                           clave contra el mismo dataset de origen).
 * @param tablaIdentificador identificador de la tabla/hoja donde ocurre
 *                           esta aparición, para poder escribir un
 *                           mensaje de advertencia útil.
 * @param numeroDeAparicion  cuántas veces (contando esta) se ha
 *                           consultado esta clave en la corrida actual.
 *                           El motor solo invoca la regla a partir de 2;
 *                           la primera aparición se resuelve de forma
 *                           normal, sin pasar por aquí.
 */
public record ContextoFila(
        String clave,
        FuenteConsumo fuente,
        double consumoMb,
        String tablaIdentificador,
        int numeroDeAparicion) {

    public ContextoFila {
        Objects.requireNonNull(clave, "clave no puede ser null");
        Objects.requireNonNull(fuente, "fuente no puede ser null");
        Objects.requireNonNull(tablaIdentificador, "tablaIdentificador no puede ser null");
        if (numeroDeAparicion < 2) {
            throw new IllegalArgumentException(
                    "numeroDeAparicion debe ser >= 2; la primera aparición no pasa por ReglaAmbigua");
        }
    }
}