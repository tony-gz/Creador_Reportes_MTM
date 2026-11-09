package com.simreports.core.matching;

import java.util.Objects;

/**
 * Advertencia de inconsistencia de totales para una fuente — spec 03,
 * "Reporte de consistencia de totales". Señala que la suma de lo
 * asignado en la plantilla más lo reportado como "no encontrado" no
 * cuadra con el total original del dataset de origen, más allá del
 * margen de tolerancia por redondeo de {@code double}. Los MB repetidos a
 * propósito entre hojas (D-001, {@code totalRepetidoEntreHojas}) se
 * descuentan de lo asignado antes de comparar.
 *
 * <p>Es la red de seguridad más barata contra un bug de matching que
 * pierda o duplique MB reales de forma silenciosa — el escenario más
 * costoso en este dominio, porque el cliente sí lo nota en su factura
 * aunque el sistema no marque ningún error explícito.</p>
 */
public record AdvertenciaConsistencia(
        FuenteConsumo fuente,
        double totalOrigen,
        double totalAsignado,
        double totalNoEncontrado,
        double totalRepetidoEntreHojas) {

    public AdvertenciaConsistencia {
        Objects.requireNonNull(fuente, "fuente no puede ser null");
    }

    /**
     * Diferencia {@code totalOrigen - (totalAsignado - totalRepetidoEntreHojas
     * + totalNoEncontrado)}.
     */
    public double diferencia() {
        return totalOrigen - (totalAsignado - totalRepetidoEntreHojas + totalNoEncontrado);
    }
}