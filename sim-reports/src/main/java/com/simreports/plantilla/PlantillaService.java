package com.simreports.plantilla;

import com.simreports.core.plantilla.LectorPlantilla;
import com.simreports.core.plantilla.MapeoPlantilla;
import com.simreports.core.plantilla.PlantillaInvalidaException;
import com.simreports.core.plantilla.PlantillaLeida;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Lee la plantilla que se sube en cada corrida — D-023 (spec 12): la app no
 * la guarda, ni a ella ni a su mapeo. Puro salvo por ser un bean.
 */
@Service
public class PlantillaService {

    private final LectorPlantilla lector = new LectorPlantilla();

    /**
     * Cómo lee la app la plantilla (hojas, tablas, columnas), sin guardarla.
     *
     * @throws PlantillaInvalidaException con <b>todos</b> los problemas.
     */
    public MapeoPlantilla revisar(byte[] contenido) {
        return lector.detectar(new ByteArrayInputStream(validar(contenido)));
    }

    /** Detecta el mapeo y lee las filas, listas para el motor. */
    public PlantillaLeida leer(byte[] contenido) {
        MapeoPlantilla mapeo = revisar(contenido);
        return lector.leer(new ByteArrayInputStream(contenido), mapeo);
    }

    private static byte[] validar(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new PlantillaInvalidaException(List.of("El archivo de la plantilla está vacío."));
        }
        return contenido;
    }
}
