package com.simreports.core.matching;

import java.util.Locale;

/**
 * Operador de una fila de la plantilla, leído de la columna {@code SIM} /
 * {@code Carrier} — decisión D-016 (spec 10). Decide dónde se busca la SIM
 * en las tablas simples:
 *
 * <ul>
 *   <li>{@link #ALESTRA}: {@code ALESTRA} o {@code MULTICARRIER} → solo por
 *       {@code LINEA} contra Alestra.</li>
 *   <li>{@link #TELCEL_ATT}: {@code TELCEL} o {@code ATT} (también
 *       {@code AT&T}, {@code AT&T_2}, {@code ATT001}...) → solo por
 *       {@code ICCID} contra Telcel + ATT.</li>
 *   <li>{@link #NO_RECONOCIDO}: vacío u otro valor ({@code STARLINK},
 *       {@code OUI}...) → se busca según el tipo de tabla, como antes de
 *       D-016 (en {@code SIMPLE_CASCADA}: línea y, si no está en Alestra,
 *       ICCID).</li>
 * </ul>
 *
 * <p>Para comparar se usan solo las letras, sin distinguir mayúsculas:
 * {@code "AT&T_2"} y {@code "ATT001"} quedan como {@code ATT}.</p>
 */
public enum OperadorPlantilla {

    ALESTRA,
    TELCEL_ATT,
    NO_RECONOCIDO;

    public static OperadorPlantilla desde(String valorColumnaSim) {
        if (valorColumnaSim == null) {
            return NO_RECONOCIDO;
        }
        String letras = valorColumnaSim.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        return switch (letras) {
            case "ALESTRA", "MULTICARRIER" -> ALESTRA;
            case "TELCEL", "ATT" -> TELCEL_ATT;
            default -> NO_RECONOCIDO;
        };
    }
}
