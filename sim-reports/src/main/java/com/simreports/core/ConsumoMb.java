package com.simreports.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Precisión de los consumos del reporte (D-022): <b>dos decimales, truncados,
 * sin redondear</b>, como vienen en la BD. P. ej. {@code 208.999 → 208.99} y
 * {@code 4321.9876 → 4321.98}.
 *
 * <p>Se aplica una sola vez, al entrar los consumos al motor de coincidencia,
 * para que los totales, la consistencia, el reporte final y el .xlsx usen
 * exactamente los mismos números.</p>
 */
public final class ConsumoMb {

    /** Decimales que se conservan. */
    public static final int DECIMALES = 2;

    /**
     * Antes de truncar se quita el ruido de punto flotante de sumas como
     * {@code 0.1 + 0.2}; si no, {@code 28.339999999} quedaría en 28.33 en vez
     * de 28.34. Seis decimales de MB son un byte.
     */
    private static final int DECIMALES_SIN_RUIDO = 6;

    private ConsumoMb() {
    }

    public static double truncar(double mb) {
        if (Double.isNaN(mb) || Double.isInfinite(mb)) {
            return mb;
        }
        return BigDecimal.valueOf(mb)
                .setScale(DECIMALES_SIN_RUIDO, RoundingMode.HALF_EVEN)
                .setScale(DECIMALES, RoundingMode.DOWN)
                .doubleValue();
    }

    /** Copia del mapa clave → MB con cada consumo truncado. */
    public static Map<String, Double> truncar(Map<String, Double> consumos) {
        Map<String, Double> truncados = new HashMap<>(consumos.size() * 2);
        consumos.forEach((clave, mb) -> truncados.put(clave, truncar(mb)));
        return truncados;
    }
}
