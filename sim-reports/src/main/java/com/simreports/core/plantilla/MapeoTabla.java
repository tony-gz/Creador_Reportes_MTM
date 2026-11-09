package com.simreports.core.plantilla;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mapeo de una tabla nativa de la plantilla (spec 01): dónde está y qué
 * encabezado real corresponde a cada campo lógico. Es lo que se persiste
 * por versión en {@code PlantillaConfig.mapeoColumnas} (spec 04) y lo que se
 * puede corregir a mano; al generar un reporte, las filas se leen con este
 * mapeo, no se vuelve a detectar.
 *
 * @param hoja                     nombre de la hoja, tal cual (puede tener
 *                                 espacios al final, p. ej. {@code "PETCO "}).
 * @param nombreTabla              nombre de la tabla nativa de Excel
 *                                 (p. ej. {@code Tabla2}).
 * @param rango                    rango de la tabla al detectarla (p. ej.
 *                                 {@code A6:I30}), solo informativo.
 * @param tipo                     tipo de tabla clasificado.
 * @param columnaPorCampoLogico    campo lógico → encabezado real.
 * @param encabezadosNoReconocidos encabezados que no son campo lógico.
 */
public record MapeoTabla(
        String hoja,
        String nombreTabla,
        String rango,
        TipoTablaPlantilla tipo,
        Map<String, String> columnaPorCampoLogico,
        List<String> encabezadosNoReconocidos) {

    public MapeoTabla {
        Objects.requireNonNull(hoja, "hoja no puede ser null");
        Objects.requireNonNull(nombreTabla, "nombreTabla no puede ser null");
        Objects.requireNonNull(rango, "rango no puede ser null");
        Objects.requireNonNull(tipo, "tipo no puede ser null");
        columnaPorCampoLogico = Map.copyOf(Objects.requireNonNull(columnaPorCampoLogico,
                "columnaPorCampoLogico no puede ser null"));
        encabezadosNoReconocidos = List.copyOf(Objects.requireNonNull(encabezadosNoReconocidos,
                "encabezadosNoReconocidos no puede ser null"));
    }

    /** {@code "HOJA · Tabla"}, para mensajes e identificador de la tabla en el motor. */
    public String identificador() {
        return hoja.strip() + " · " + nombreTabla;
    }
}
