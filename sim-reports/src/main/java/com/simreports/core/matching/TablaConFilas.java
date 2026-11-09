package com.simreports.core.matching;

import com.simreports.core.plantilla.TipoTablaPlantilla;

import java.util.List;
import java.util.Objects;

/**
 * Una tabla de la plantilla ya resuelta (spec 01) junto con las filas de
 * datos leídas de esa tabla en el Excel. Es la unidad de entrada que
 * consume el motor de coincidencia por cada una de las tablas de las 18
 * hojas — spec 03, "Contrato de entrada del motor".
 *
 * @param hoja           nombre de la hoja de Excel donde vive la tabla. Sí
 *                       se usa en el matching: una SIM repetida en la
 *                       <b>misma hoja</b> se cuenta una vez y una repetida
 *                       en <b>hojas distintas</b> se cuenta en cada una
 *                       (decisiones D-001 y D-006, spec 10). Dos tablas de
 *                       la misma hoja (p. ej. los dos bloques de ALSEA)
 *                       deben llevar el mismo {@code hoja}.
 * @param identificador nombre descriptivo de la tabla, solo para
 *                       trazabilidad en reportes (p. ej.
 *                       {@code "BIMBO - Table1"}). No se usa para
 *                       ninguna decisión de matching.
 * @param tipo           tipo ya clasificado por
 *                       {@link com.simreports.core.plantilla.ResolverTablaPlantilla}.
 * @param filas          filas de la tabla, en el orden en que aparecen
 *                       en Excel (el orden no afecta el resultado del
 *                       matching, solo se preserva para que el resultado
 *                       de {@link MotorCoincidencia#resolverTabla} se
 *                       pueda mapear de vuelta a la fila correcta al
 *                       escribir en el Excel).
 */
public record TablaConFilas(String hoja, String identificador, TipoTablaPlantilla tipo, List<FilaPlantilla> filas) {

    public TablaConFilas {
        Objects.requireNonNull(hoja, "hoja no puede ser null");
        Objects.requireNonNull(identificador, "identificador no puede ser null");
        Objects.requireNonNull(tipo, "tipo no puede ser null");
        Objects.requireNonNull(filas, "filas no puede ser null");
        filas = List.copyOf(filas);
    }

    /**
     * Tabla que es la única de su hoja: el identificador se usa también como
     * nombre de hoja. Cómodo en pruebas; el lector de la plantilla debe usar
     * el constructor completo, porque una hoja puede tener varias tablas.
     */
    public TablaConFilas(String identificador, TipoTablaPlantilla tipo, List<FilaPlantilla> filas) {
        this(identificador, identificador, tipo, filas);
    }
}