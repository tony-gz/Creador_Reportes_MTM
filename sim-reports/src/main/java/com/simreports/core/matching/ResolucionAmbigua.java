package com.simreports.core.matching;

import java.util.Objects;
import java.util.Optional;

/**
 * Resultado de aplicar una {@link ReglaAmbigua} a una aparición repetida
 * de una clave — spec 03.
 *
 * @param asignacion  qué escribir en esta fila para esta clave. Una vez
 *                    que una clave es ambigua, el motor delega
 *                    completamente en la regla: no aplica ningún
 *                    comportamiento propio además de este valor.
 * @param advertencia mensaje a agregar a los avisos de la corrida (el
 *                    motor lo envuelve como {@code AvisoParseo} de tipo
 *                    {@code CLAVE_DUPLICADA}), o {@code null} si esta
 *                    aparición no amerita aviso.
 */
public record ResolucionAmbigua(Asignacion asignacion, String advertencia) {

    public ResolucionAmbigua {
        Objects.requireNonNull(asignacion, "asignacion no puede ser null");
    }

    public Optional<String> advertenciaOpcional() {
        return Optional.ofNullable(advertencia);
    }
}