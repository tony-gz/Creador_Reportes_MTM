package com.simreports.core.plantilla;

import java.util.List;
import java.util.Objects;

/**
 * Mapeo de todas las tablas de clientes de una plantilla, en el orden de las
 * hojas y, dentro de cada hoja, en el orden en que Excel devuelve sus tablas.
 */
public record MapeoPlantilla(List<MapeoTabla> tablas) {

    public MapeoPlantilla {
        tablas = List.copyOf(Objects.requireNonNull(tablas, "tablas no puede ser null"));
    }
}
