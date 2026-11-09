package com.simreports.core.matching;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Una clave (línea o ICCID) con consumo en las fuentes que aparece más de una
 * vez en la plantilla — reporte final de la corrida (spec 10, D-006).
 *
 * @param fuente      fuente donde matcheó la clave.
 * @param clave       línea o ICCID ya normalizado.
 * @param consumoMb   MB de la fuente para esta clave.
 * @param apariciones todas las apariciones, en el orden en que el motor las
 *                    procesó (2 o más).
 */
public record SimRepetida(FuenteConsumo fuente, String clave, double consumoMb, List<AparicionSim> apariciones) {

    public SimRepetida {
        Objects.requireNonNull(fuente, "fuente no puede ser null");
        Objects.requireNonNull(clave, "clave no puede ser null");
        Objects.requireNonNull(apariciones, "apariciones no puede ser null");
        apariciones = List.copyOf(apariciones);
        if (apariciones.size() < 2) {
            throw new IllegalArgumentException("una SIM repetida necesita al menos 2 apariciones");
        }
    }

    /** Hojas donde aparece, sin repetir, en orden de aparición. */
    public List<String> hojas() {
        Set<String> hojas = new LinkedHashSet<>();
        for (AparicionSim a : apariciones) {
            hojas.add(a.hoja());
        }
        return List.copyOf(hojas);
    }

    /** {@code true} si alguna hoja la tiene dos o más veces (D-006). */
    public boolean repetidaEnLaMismaHoja() {
        return hojas().size() < apariciones.size();
    }

    /** {@code true} si aparece en dos o más hojas (D-001). */
    public boolean repetidaEntreHojas() {
        return hojas().size() > 1;
    }
}
