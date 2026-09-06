package com.simreports.core.plantilla;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resultado de resolver una tabla de la plantilla — ver spec 01, sección
 * 3 ("Resolver de tablas de la plantilla").
 *
 * @param tipo                    tipo de tabla, ya clasificado.
 * @param columnaPorCampoLogico   mapa campo lógico (p. ej. {@code
 *                                "LINEA"}, {@code "ICCID"},
 *                                {@code "CONSUMO(MB)"}) → encabezado
 *                                real tal cual aparece en la tabla de
 *                                Excel (sin normalizar). Se necesita el
 *                                encabezado real, no el normalizado,
 *                                porque es lo que se usa después para
 *                                ubicar la columna al leer/escribir
 *                                celdas con Apache POI.
 * @param encabezadosNoReconocidos encabezados reales de la tabla que no
 *                                resolvieron a ningún campo lógico (p.
 *                                ej. {@code SIM}, {@code SERIE}, o
 *                                {@code CONSUMO_TOTAL} en tablas dual
 *                                SIM, que se recalcula y nunca se lee).
 *                                No bloquea el proceso — se persiste en
 *                                el mapeo para revisión manual en la UI
 *                                (spec 01, sección "Persistencia del
 *                                mapeo").
 */
public record TablaPlantillaResuelta(
        TipoTablaPlantilla tipo,
        Map<String, String> columnaPorCampoLogico,
        List<String> encabezadosNoReconocidos) {

    public TablaPlantillaResuelta {
        Objects.requireNonNull(tipo, "tipo no puede ser null");
        Objects.requireNonNull(columnaPorCampoLogico, "columnaPorCampoLogico no puede ser null");
        Objects.requireNonNull(encabezadosNoReconocidos, "encabezadosNoReconocidos no puede ser null");
        columnaPorCampoLogico = Map.copyOf(columnaPorCampoLogico);
        encabezadosNoReconocidos = List.copyOf(encabezadosNoReconocidos);
    }
}
