package com.simreports.plantilla;

import java.util.Map;
import java.util.Objects;

/**
 * Cuerpo de {@code PUT /api/plantilla/mapeo}: qué tabla se corrige y su mapeo
 * completo nuevo (campo lógico → encabezado real). Ejemplo:
 * <pre>{"hoja":"GEPP","nombreTabla":"Tabla3",
 *  "columnaPorCampoLogico":{"LINEA":"Número","ICCID":"ICCID","CONSUMO(MB)":"CONSUMO(MB)"}}</pre>
 */
public record CorreccionMapeoSolicitud(String hoja, String nombreTabla, Map<String, String> columnaPorCampoLogico) {

    public CorreccionMapeoSolicitud {
        Objects.requireNonNull(hoja, "falta 'hoja'");
        Objects.requireNonNull(nombreTabla, "falta 'nombreTabla'");
        Objects.requireNonNull(columnaPorCampoLogico, "falta 'columnaPorCampoLogico'");
    }
}
