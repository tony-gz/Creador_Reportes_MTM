package com.simreports.reportes;

import java.util.Objects;

/** El .xlsx de una corrida, con el nombre con que se descarga. */
public record ArchivoDescarga(String nombre, byte[] contenido) {

    public ArchivoDescarga {
        Objects.requireNonNull(nombre, "nombre no puede ser null");
        Objects.requireNonNull(contenido, "contenido no puede ser null");
    }
}
