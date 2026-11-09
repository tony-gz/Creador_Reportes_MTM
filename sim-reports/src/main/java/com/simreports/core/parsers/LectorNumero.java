package com.simreports.core.parsers;

import com.simreports.core.NormalizadorClave;

import java.util.regex.Pattern;

/**
 * Convierte a número un consumo que llegó como texto (celda de texto en
 * un .xlsx, o cualquier valor de un .csv). Compartido por todos los
 * parsers de origen para que "1,234.5" signifique lo mismo en Alestra,
 * Telcel y ATT.
 *
 * <p>Formatos aceptados:</p>
 * <ul>
 *   <li>Simple: {@code 1234.5}, {@code -3}, {@code .5}, {@code 1.5E-3}.</li>
 *   <li>Miles con coma y decimal con punto (convención de México):
 *       {@code 1,234.5}, {@code 1,234,567}.</li>
 *   <li>Miles con punto y decimal con coma: {@code 1.234,5}.</li>
 *   <li>Decimal con coma, sin miles: {@code 1,5}, {@code 0,004},
 *       {@code 12345,6}.</li>
 * </ul>
 *
 * <p>Caso ambiguo: {@code 1,234} (una sola coma seguida de exactamente 3
 * dígitos) se interpreta como <b>mil doscientos treinta y cuatro</b>, la
 * convención de los exports mexicanos — igual que antes de esta clase.
 * Un grupo inicial {@code 0} nunca es de miles: {@code 0,004} es
 * {@code 0.004}.</p>
 *
 * <p>Cualquier otro texto ({@code NaN}, {@code Infinity}, {@code 10d},
 * {@code 1,2,3}, letras) devuelve {@code null}: el parser excluye la fila
 * con un aviso en vez de inventar un valor. {@link Double#parseDouble}
 * por sí solo aceptaría algunos de esos textos.</p>
 */
final class LectorNumero {

    private static final Pattern SIMPLE = Pattern.compile("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$");
    private static final Pattern MILES_COMA_DECIMAL_PUNTO = Pattern.compile("^[+-]?[1-9]\\d{0,2}(,\\d{3})+(\\.\\d+)?$");
    private static final Pattern MILES_PUNTO_DECIMAL_COMA = Pattern.compile("^[+-]?[1-9]\\d{0,2}(\\.\\d{3})+,\\d+$");
    private static final Pattern DECIMAL_COMA = Pattern.compile("^[+-]?\\d+,\\d+$");
    private static final Pattern ESPACIOS = Pattern.compile("[\\s\\p{Z}\\uFEFF\\u200B]+");

    private LectorNumero() {
    }

    static Double parsear(String texto) {
        if (texto == null) {
            return null;
        }
        String v = NormalizadorClave.quitarEspaciosExtremos(texto);
        if (v.length() >= 2 && v.startsWith("'") && v.endsWith("'")) {
            v = v.substring(1, v.length() - 1);
        }
        // Espacios internos: separador de miles en algunos formatos
        // ("1 234,5", a veces con NBSP).
        v = ESPACIOS.matcher(v).replaceAll("");
        if (v.isEmpty()) {
            return null;
        }

        String normalizado;
        if (SIMPLE.matcher(v).matches()) {
            normalizado = v;
        } else if (MILES_COMA_DECIMAL_PUNTO.matcher(v).matches()) {
            normalizado = v.replace(",", "");
        } else if (MILES_PUNTO_DECIMAL_COMA.matcher(v).matches()) {
            normalizado = v.replace(".", "").replace(',', '.');
        } else if (DECIMAL_COMA.matcher(v).matches()) {
            normalizado = v.replace(',', '.');
        } else {
            return null;
        }

        try {
            return Double.parseDouble(normalizado);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
