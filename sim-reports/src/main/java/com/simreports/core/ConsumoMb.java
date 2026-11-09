package com.simreports.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Precisión de los consumos del reporte (D-022): <b>dos decimales,
 * redondeados</b> como lo hace Excel (de 5 en adelante sube), para no regalar
 * MB. P. ej. {@code 369.1373 → 369.14}, {@code 208.999 → 209.00} y
 * {@code 0.284 → 0.28}.
 *
 * <p>Se aplica una sola vez, al entrar los consumos al motor de coincidencia,
 * para que los totales, la consistencia, el reporte final y el .xlsx usen
 * exactamente los mismos números.</p>
 */
public final class ConsumoMb {

    /** Decimales que se conservan. */
    public static final int DECIMALES = 2;

    private ConsumoMb() {
    }

    /**
     * {@link BigDecimal#valueOf(double)} parte del decimal más corto que
     * representa al {@code double} ({@code 27.39} y no {@code 27.3899999…}),
     * así que los valores que ya traen dos decimales no cambian.
     */
    public static double redondear(double mb) {
        if (Double.isNaN(mb) || Double.isInfinite(mb)) {
            return mb;
        }
        return BigDecimal.valueOf(mb).setScale(DECIMALES, RoundingMode.HALF_UP).doubleValue();
    }

    /** Copia del mapa clave → MB con cada consumo redondeado. */
    public static Map<String, Double> redondear(Map<String, Double> consumos) {
        Map<String, Double> redondeados = new HashMap<>(consumos.size() * 2);
        consumos.forEach((clave, mb) -> redondeados.put(clave, redondear(mb)));
        return redondeados;
    }
}
