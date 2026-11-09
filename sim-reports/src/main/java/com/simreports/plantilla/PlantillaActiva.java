package com.simreports.plantilla;

import com.simreports.core.plantilla.MapeoPlantilla;

import java.util.Objects;

/** Una versión de la plantilla junto con su mapeo ya leído del JSON. */
public record PlantillaActiva(PlantillaConfig version, MapeoPlantilla mapeo) {

    public PlantillaActiva {
        Objects.requireNonNull(version, "version no puede ser null");
        Objects.requireNonNull(mapeo, "mapeo no puede ser null");
    }
}
