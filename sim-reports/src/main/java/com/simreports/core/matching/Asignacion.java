package com.simreports.core.matching;

import java.util.Optional;

/**
 * Resultado de intentar resolver una sola clave (línea o ICCID) contra
 * los datasets de origen — spec 03, sección "Coincidencias no
 * encontradas".
 *
 * @param consumoMb MB encontrados, o {@code 0.0} si no hubo match. Nunca
 *                  se representa la ausencia de match con {@code null}:
 *                  el motor nunca deja una celda vacía ni propaga un
 *                  error, misma semántica que
 *                  {@code SI.ERROR(BUSCARV(...),0)} del proceso manual
 *                  actual.
 * @param fuente    de qué fuente vino el match, o {@code null} si no
 *                  hubo match. Un match con {@code 0.0} MB reales
 *                  reportados por el operador sigue siendo un match
 *                  (fuente no nula) para efectos de "no encontrados": la
 *                  clave sí fue consultada y sí existe en el origen.
 */
public record Asignacion(double consumoMb, FuenteConsumo fuente) {

    public static Asignacion sinMatch() {
        return new Asignacion(0.0, null);
    }

    public boolean tieneMatch() {
        return fuente != null;
    }

    public Optional<FuenteConsumo> fuenteOpcional() {
        return Optional.ofNullable(fuente);
    }
}