package com.simreports.core;

import java.util.Locale;
import java.util.Optional;

/**
 * Contrato común para resolver un encabezado real de una hoja/archivo a
 * un campo lógico conocido — ver spec 01, sección "Interfaz
 * HeaderResolver (reutilizable en spec 02)".
 *
 * <p>Se reutiliza tanto para la plantilla (implementación con la lista
 * fija de campos lógicos de spec 01) como para cada uno de los tres
 * parsers de origen (Alestra, Telcel, ATT — spec 02), cada uno con su
 * propia lista fija de campos esperados. Todas las implementaciones
 * comparten la misma normalización ({@link #normalizar(String)}) y el
 * mismo comportamiento ante un encabezado no reconocido: nunca lanzan
 * excepción por esto — devuelven {@link Optional#empty()} y quien llama
 * decide qué hacer (ignorar la columna, o acumularla como "no
 * reconocida" para mostrarla en la UI).</p>
 */
public interface HeaderResolver {

    /**
     * Resuelve un encabezado real al campo lógico correspondiente.
     *
     * @param headerReal el texto del encabezado tal como viene en el
     *                    archivo/hoja (puede traer espacios de más,
     *                    mayúsculas/minúsculas mezcladas, etc.).
     * @return el campo lógico normalizado (p. ej. {@code "ICCID"},
     *         {@code "CONSUMO(MB)"}) si se reconoce, o
     *         {@link Optional#empty()} si no coincide con ningún campo
     *         esperado por esta implementación.
     */
    Optional<String> resolver(String headerReal);

    /**
     * Normalización compartida por todas las implementaciones de
     * {@link HeaderResolver} (plantilla y los tres parsers de origen) —
     * ver spec 01, sección "Campos lógicos y normalización".
     *
     * <p>Importante: elimina <b>todos</b> los espacios internos, no solo
     * los colapsa a uno. Esto se ajustó con datos reales de Fase 0: 3 de
     * las 18 hojas de la plantilla real traen {@code "CONSUMO (MB)"} con
     * un espacio antes del paréntesis, que una normalización que solo
     * colapsara espacios (a un único espacio) no habría hecho coincidir
     * con {@code "CONSUMO(MB)"}.</p>
     *
     * <p>"Espacio" incluye también los caracteres invisibles que Excel y
     * los exports suelen colar en un encabezado y que {@code \s} de Java
     * no cubre: espacio de no separación ({@code U+00A0}) y demás
     * separadores Unicode ({@code \p{Z}}), la marca de orden de bytes
     * ({@code U+FEFF}, típica al inicio de un .csv guardado como "UTF-8
     * con BOM") y el espacio de ancho cero ({@code U+200B}).</p>
     *
     * @param header encabezado real, puede ser {@code null}.
     * @return el encabezado en mayúsculas, sin espacios al inicio/final
     *         ni internos. Cadena vacía si {@code header} es
     *         {@code null} o solo contenía espacios.
     */
    static String normalizar(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s\\p{Z}\\uFEFF\\u200B]+", "");
    }
}
