package com.simreports.plantilla;

import com.simreports.core.plantilla.PlantillaLeida;

import java.util.Objects;

/**
 * La plantilla activa lista para generar un reporte (Fase 2 punto 4): la
 * versión usada (para {@code ReporteGenerado.plantillaVersionId}), su
 * contenido (el escritor parte de este archivo) y sus filas leídas con el
 * mapeo guardado.
 */
public record PlantillaParaReporte(PlantillaConfig version, byte[] contenido, PlantillaLeida leida) {

    public PlantillaParaReporte {
        Objects.requireNonNull(version, "version no puede ser null");
        Objects.requireNonNull(contenido, "contenido no puede ser null");
        Objects.requireNonNull(leida, "leida no puede ser null");
    }
}
