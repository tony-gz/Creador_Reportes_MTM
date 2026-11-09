package com.simreports.reportes;

import java.util.Objects;

/** Un archivo de operador subido para una corrida: nombre original (solo trazabilidad) y contenido. */
public record ArchivoEntrada(String nombre, byte[] contenido) {

    public ArchivoEntrada {
        nombre = nombre == null || nombre.isBlank() ? "(sin nombre)" : nombre;
        Objects.requireNonNull(contenido, "contenido no puede ser null");
    }
}
