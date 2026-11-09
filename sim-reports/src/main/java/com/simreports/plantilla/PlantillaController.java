package com.simreports.plantilla;

import com.simreports.core.plantilla.PlantillaInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/**
 * Endpoints de la plantilla — spec 04 y Fase 2 punto 3 (spec 08). Los errores
 * los traduce {@code ManejadorErrores}: plantilla con problemas → 400 con la
 * lista {@code problemas}; sin plantilla activa → 409.
 */
@RestController
@RequestMapping("/api/plantilla")
public class PlantillaController {

    private final PlantillaService servicio;

    public PlantillaController(PlantillaService servicio) {
        this.servicio = Objects.requireNonNull(servicio, "servicio no puede ser null");
    }

    /** Sube una plantilla nueva (campo de formulario {@code archivo}); queda como versión activa. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PlantillaRespuesta subir(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new PlantillaInvalidaException(List.of("El archivo está vacío."));
        }
        try {
            return PlantillaRespuesta.de(servicio.registrarNuevaVersion(archivo.getBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo subido", e);
        }
    }

    /** Mapeo de la versión activa. */
    @GetMapping("/mapeo")
    public PlantillaRespuesta mapeo() {
        return PlantillaRespuesta.de(servicio.activa().orElseThrow(SinPlantillaActivaException::new));
    }

    /** Corrige a mano el mapeo de una tabla de la versión activa (no crea versión nueva). */
    @PutMapping("/mapeo")
    public PlantillaRespuesta corregirMapeo(@RequestBody CorreccionMapeoSolicitud solicitud) {
        return PlantillaRespuesta.de(servicio.corregirMapeo(
                solicitud.hoja(), solicitud.nombreTabla(), solicitud.columnaPorCampoLogico()));
    }

    /** Historial de versiones, la más reciente primero. */
    @GetMapping("/versiones")
    public List<VersionPlantillaRespuesta> versiones() {
        return servicio.versiones().stream().map(VersionPlantillaRespuesta::de).toList();
    }
}
