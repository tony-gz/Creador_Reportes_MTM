package com.simreports.core.plantilla;

import com.simreports.core.matching.TablaConFilas;

import java.util.Map;
import java.util.Objects;

/**
 * Una tabla de la plantilla ya leída: su mapeo, las filas listas para el motor
 * ({@link TablaConFilas}, cada {@code FilaPlantilla} con su fila de Excel) y
 * el índice de columna (0 = columna A) de cada campo lógico, para que el
 * escritor del reporte sepa en qué celdas poner el consumo.
 */
public record TablaLeida(MapeoTabla mapeo, TablaConFilas tablaConFilas, Map<String, Integer> indiceColumnaPorCampoLogico) {

    public TablaLeida {
        Objects.requireNonNull(mapeo, "mapeo no puede ser null");
        Objects.requireNonNull(tablaConFilas, "tablaConFilas no puede ser null");
        indiceColumnaPorCampoLogico = Map.copyOf(Objects.requireNonNull(indiceColumnaPorCampoLogico,
                "indiceColumnaPorCampoLogico no puede ser null"));
    }
}
