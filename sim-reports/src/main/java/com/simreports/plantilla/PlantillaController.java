package com.simreports.plantilla;

import com.simreports.core.plantilla.MapeoPlantilla;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Revisar la plantilla sin guardarla — D-023 (spec 12). Con problemas → 400
 * con la lista {@code problemas} ({@code ManejadorErrores}).
 */
@RestController
@RequestMapping("/api/plantilla")
public class PlantillaController {

    private final PlantillaService servicio;

    public PlantillaController(PlantillaService servicio) {
        this.servicio = Objects.requireNonNull(servicio, "servicio no puede ser null");
    }

    /** Cómo lee la app la plantilla del campo {@code plantilla}: hojas, tablas y columnas. */
    @PostMapping(path = "/revisar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MapeoPlantilla revisar(@RequestParam("plantilla") MultipartFile plantilla) {
        try {
            return servicio.revisar(plantilla.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo subido", e);
        }
    }
}
