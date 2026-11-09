package com.simreports.plantilla;

import java.time.Instant;

/** Una versión de la plantilla en las respuestas de la API (sin la ruta en disco). */
public record VersionPlantillaRespuesta(Long id, int version, boolean activa, Instant fechaCreacion) {

    static VersionPlantillaRespuesta de(PlantillaConfig config) {
        return new VersionPlantillaRespuesta(config.getId(), config.getVersion(), config.isActiva(),
                config.getFechaCreacion());
    }
}
