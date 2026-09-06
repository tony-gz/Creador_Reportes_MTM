package com.simreports.core.matching;

import java.util.Objects;

/**
 * Una clave (línea o ICCID) de un dataset de origen que nunca matcheó
 * contra ninguna tabla de la plantilla — spec 03, "Reporte secundario:
 * no encontrados". Útil como alerta temprana de líneas nuevas sin dar de
 * alta, o typos en la plantilla.
 */
public record NoEncontrado(FuenteConsumo fuente, String clave, double consumoMb) {

    public NoEncontrado {
        Objects.requireNonNull(fuente, "fuente no puede ser null");
        Objects.requireNonNull(clave, "clave no puede ser null");
    }
}