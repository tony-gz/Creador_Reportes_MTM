package com.simreports.plantilla;

import com.simreports.core.plantilla.MapeoPlantilla;

/** Versión de la plantilla con su mapeo, para {@code POST /api/plantilla} y {@code GET}/{@code PUT /mapeo}. */
public record PlantillaRespuesta(VersionPlantillaRespuesta version, MapeoPlantilla mapeo) {

    static PlantillaRespuesta de(PlantillaActiva activa) {
        return new PlantillaRespuesta(VersionPlantillaRespuesta.de(activa.version()), activa.mapeo());
    }
}
