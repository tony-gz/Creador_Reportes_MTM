package com.simreports.core.plantilla;

import com.simreports.core.matching.TablaConFilas;

import java.util.List;
import java.util.Objects;

/** Todas las tablas de una plantilla leídas con su mapeo, en orden. */
public record PlantillaLeida(List<TablaLeida> tablas) {

    public PlantillaLeida {
        tablas = List.copyOf(Objects.requireNonNull(tablas, "tablas no puede ser null"));
    }

    /** Las tablas en el formato que recibe {@code MotorCoincidencia.resolverTablas}. */
    public List<TablaConFilas> tablasConFilas() {
        return tablas.stream().map(TablaLeida::tablaConFilas).toList();
    }
}
