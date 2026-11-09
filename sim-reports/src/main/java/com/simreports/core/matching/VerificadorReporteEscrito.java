package com.simreports.core.matching;

import java.util.Objects;
import java.util.Optional;

/**
 * Verificación independiente del reporte ya escrito — complemento de la
 * consistencia de totales de spec 03.
 *
 * <p><b>Por qué hace falta.</b> La comprobación interna de
 * {@link MotorCoincidencia#finalizar()} ({@code totalOrigen == asignado +
 * noEncontrado}) usa los mismos contadores que el motor actualiza al
 * asignar, así que con la regla ambigua default cuadra siempre por
 * construcción: no puede detectar un error al <i>escribir</i> el .xlsx (fila
 * desplazada, columna equivocada, tabla saltada, fila de totales
 * sobrescrita). Tampoco detecta claves con formato distinto entre plantilla
 * y origen: esas terminan en "no encontrados", que la UI debe mostrar.</p>
 *
 * <p><b>Cómo se usa (Fase 2, servicio de reportes).</b> Después de escribir
 * el .xlsx de salida, se vuelve a abrir el archivo escrito, se suman las
 * celdas de las columnas de consumo de todas las tablas ({@code CONSUMO(MB)},
 * {@code CONSUMO(MB)_1} y {@code CONSUMO(MB)_2}; <b>no</b>
 * {@code CONSUMO_TOTAL}, que es la suma de las dos anteriores) y se llama a
 * {@link #verificar}. Un descuadre indica un bug del escritor: el reporte no
 * debe entregarse como válido.</p>
 *
 * <p>Puro: no conoce Excel; recibe el total ya leído.</p>
 */
public final class VerificadorReporteEscrito {

    /** Tolerancia por redondeo de {@code double}, igual que en el motor. */
    static final double MARGEN_TOLERANCIA_MB = 0.01;

    private VerificadorReporteEscrito() {
    }

    /**
     * @param resultado           resultado de {@link MotorCoincidencia#finalizar()}.
     * @param totalLeidoDelReporte suma de las celdas de consumo leídas del
     *                             .xlsx ya escrito.
     * @return el descuadre, o vacío si el archivo contiene exactamente lo
     *         asignado (dentro de la tolerancia).
     */
    public static Optional<DescuadreReporteEscrito> verificar(ResultadoMatching resultado, double totalLeidoDelReporte) {
        Objects.requireNonNull(resultado, "resultado no puede ser null");
        double esperado = resultado.totalAsignado();
        if (Double.isNaN(totalLeidoDelReporte) || Math.abs(esperado - totalLeidoDelReporte) > MARGEN_TOLERANCIA_MB) {
            return Optional.of(new DescuadreReporteEscrito(esperado, totalLeidoDelReporte));
        }
        return Optional.empty();
    }
}
