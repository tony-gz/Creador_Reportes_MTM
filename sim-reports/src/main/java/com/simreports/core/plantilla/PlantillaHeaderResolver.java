package com.simreports.core.plantilla;

import com.simreports.core.HeaderResolver;

import java.util.Optional;
import java.util.Set;

/**
 * Implementación de {@link HeaderResolver} para las tablas de la
 * plantilla — ver spec 01, sección "Nomenclatura estandarizada de
 * encabezados".
 *
 * <p>Con la nomenclatura ya estandarizada en la plantilla, el campo
 * lógico resuelto es, en la práctica, el propio encabezado normalizado:
 * no hace falta un diccionario de sinónimos como en los parsers de
 * origen (spec 02), solo verificar que el encabezado normalizado sea uno
 * de los campos lógicos esperados.</p>
 *
 * <p>Única excepción: la columna del operador se llama {@code SIM} en casi
 * todas las hojas y {@code Carrier} en TIENDAS EXTRA; las dos resuelven al
 * campo lógico {@code SIM} (decisión D-016, spec 10).</p>
 */
public class PlantillaHeaderResolver implements HeaderResolver {

    /**
     * Campos lógicos válidos en la plantilla, ya normalizados (ver
     * {@link HeaderResolver#normalizar(String)}). Cubre tanto las tablas
     * simples ({@code LINEA}, {@code ICCID}, {@code CONSUMO(MB)}) como
     * las tablas dual SIM / router ({@code LINEA_1}, {@code LINEA_2},
     * {@code CONSUMO(MB)_1}, {@code CONSUMO(MB)_2}).
     */
    private static final Set<String> CAMPOS_LOGICOS = Set.of(
            "SIM",
            "LINEA",
            "LINEA_1",
            "LINEA_2",
            "ICCID",
            "CONSUMO(MB)",
            "CONSUMO(MB)_1",
            "CONSUMO(MB)_2"
    );

    /** Nombre alterno de la columna {@code SIM} (operador de la SIM). */
    private static final String SINONIMO_SIM = "CARRIER";

    @Override
    public Optional<String> resolver(String headerReal) {
        String normalizado = HeaderResolver.normalizar(headerReal);
        if (SINONIMO_SIM.equals(normalizado)) {
            return Optional.of("SIM");
        }
        if (CAMPOS_LOGICOS.contains(normalizado)) {
            return Optional.of(normalizado);
        }
        return Optional.empty();
    }
}
