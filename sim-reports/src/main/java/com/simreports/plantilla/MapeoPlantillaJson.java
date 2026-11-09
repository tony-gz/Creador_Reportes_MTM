package com.simreports.plantilla;

import com.simreports.core.plantilla.MapeoPlantilla;
import com.simreports.core.plantilla.MapeoTabla;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;

/**
 * Convierte el {@link MapeoPlantilla} al JSON que se guarda en
 * {@code PlantillaConfig.mapeoColumnas} y de vuelta (spec 04). El core no
 * conoce Jackson; la conversión vive aquí.
 *
 * <p>Formato: {@code {"formato":1,"tablas":[{hoja, nombreTabla, rango, tipo,
 * columnaPorCampoLogico, encabezadosNoReconocidos}, ...]}}. El campo
 * {@code formato} permite cambiar la estructura en el futuro sin confundir
 * los mapeos ya guardados.</p>
 */
@Component
public class MapeoPlantillaJson {

    static final int FORMATO_ACTUAL = 1;

    private final JsonMapper jsonMapper;

    public MapeoPlantillaJson(JsonMapper jsonMapper) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper no puede ser null");
    }

    public String escribir(MapeoPlantilla mapeo) {
        return jsonMapper.writeValueAsString(new MapeoGuardado(FORMATO_ACTUAL, mapeo.tablas()));
    }

    public MapeoPlantilla leer(String json) {
        MapeoGuardado guardado = jsonMapper.readValue(json, MapeoGuardado.class);
        if (guardado.formato() != FORMATO_ACTUAL) {
            throw new IllegalStateException("Formato de mapeo desconocido: " + guardado.formato());
        }
        return new MapeoPlantilla(guardado.tablas());
    }

    record MapeoGuardado(int formato, List<MapeoTabla> tablas) {
    }
}
