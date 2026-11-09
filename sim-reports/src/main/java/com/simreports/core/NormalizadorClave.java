package com.simreports.core;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Forma canónica de una clave de matching (línea o ICCID). Es el único
 * lugar del proyecto que decide cómo se limpia una clave antes de
 * compararla, y lo usan los dos lados del matching:
 * <ul>
 *   <li>los parsers de origen (Alestra, Telcel, ATT) al construir las
 *       claves de su {@code Map} de consumos;</li>
 *   <li>el motor de coincidencia al recibir las claves leídas de la
 *       plantilla ({@code FilaPlantilla}).</li>
 * </ul>
 * Si cada lado limpiara por su cuenta, una diferencia mínima (un espacio,
 * un guion, unas comillas) haría que la misma línea no coincida y sus MB
 * se vayan en silencio a "no encontrados".
 *
 * <p>Reglas, en este orden:</p>
 * <ol>
 *   <li>Quitar espacios al inicio/fin, incluidos los invisibles (NBSP,
 *       BOM, ancho cero).</li>
 *   <li>Quitar comillas simples literales que envuelven el valor (caso
 *       real del export de Telcel: {@code '8952...'}, spec 02).</li>
 *   <li>Si el valor es un entero con sufijo decimal cero
 *       ({@code 5512345678.0}, típico de un número leído como
 *       {@code double} y pasado a texto), quitar el sufijo.</li>
 *   <li>Quitar separadores de formato: cualquier espacio (también los
 *       invisibles), guiones y paréntesis — {@code (55) 1234-5678} y
 *       {@code 55 1234 5678} quedan como {@code 5512345678}.</li>
 *   <li>Pasar a mayúsculas y quitar la {@code F} de relleno al final de un
 *       ICCID de 19 dígitos ({@code 8952000000000012300F} →
 *       {@code 8952000000000012300}). La {@code F} es relleno hexadecimal
 *       que algunos sistemas agregan para completar 20 posiciones; la
 *       plantilla real la trae en los ICCID de Telcel y Alestra, pero el
 *       export de Telcel no — sin quitarla, esas SIMs nunca coincidirían.</li>
 * </ol>
 *
 * <p>Deliberadamente <b>no</b> quita prefijos de país ({@code +52},
 * {@code 52}, {@code 521}): si la plantilla y Alestra usan longitudes de
 * línea distintas, es una decisión de negocio que se toma con datos
 * reales, no algo que esta clase deba adivinar.</p>
 *
 * <p>Es idempotente: normalizar dos veces da el mismo resultado.</p>
 */
public final class NormalizadorClave {

    private static final Pattern ESPACIOS_EXTREMOS =
            Pattern.compile("^[\\s\\p{Z}\\uFEFF\\u200B]+|[\\s\\p{Z}\\uFEFF\\u200B]+$");
    private static final Pattern ENTERO_CON_DECIMAL_CERO = Pattern.compile("^(\\d+)\\.0+$");
    private static final Pattern SEPARADORES = Pattern.compile("[\\s\\p{Z}\\uFEFF\\u200B\\-()]+");
    private static final Pattern ICCID_CON_F_DE_RELLENO = Pattern.compile("^(\\d{19})F$");

    private NormalizadorClave() {
    }

    /**
     * @param valor clave tal como viene de la celda o del CSV; puede ser
     *              {@code null}.
     * @return la clave canónica, o cadena vacía si {@code valor} es
     *         {@code null} o no queda ningún carácter útil.
     */
    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String v = quitarEspaciosExtremos(valor);
        if (v.length() >= 2 && v.startsWith("'") && v.endsWith("'")) {
            v = quitarEspaciosExtremos(v.substring(1, v.length() - 1));
        }
        v = ENTERO_CON_DECIMAL_CERO.matcher(v).replaceFirst("$1");
        v = SEPARADORES.matcher(v).replaceAll("").toUpperCase(Locale.ROOT);
        return ICCID_CON_F_DE_RELLENO.matcher(v).replaceFirst("$1");
    }

    /**
     * Quita espacios al inicio/fin, incluidos los invisibles (NBSP, BOM,
     * ancho cero), que {@link String#trim()} no cubre.
     */
    public static String quitarEspaciosExtremos(String valor) {
        return ESPACIOS_EXTREMOS.matcher(valor).replaceAll("");
    }
}
