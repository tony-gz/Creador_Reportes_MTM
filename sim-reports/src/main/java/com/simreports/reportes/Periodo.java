package com.simreports.reportes;

import java.util.List;

/**
 * Mes y año que reporta una corrida. Da el nombre del archivo generado, como
 * lo nombra hoy el usuario: {@code CONSUMOS_OCTUBRE_2026.xlsx} (decisión del
 * 2026-11-09).
 */
public record Periodo(int anio, int mes) {

    private static final List<String> MESES = List.of("ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE");

    public Periodo {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12; llegó " + mes + ".");
        }
        if (anio < 2000 || anio > 2100) {
            throw new IllegalArgumentException("El año debe estar entre 2000 y 2100; llegó " + anio + ".");
        }
    }

    /** {@code CONSUMOS_<MES>_<AÑO>.xlsx}, p. ej. {@code CONSUMOS_OCTUBRE_2026.xlsx}. */
    public String nombreArchivo() {
        return "CONSUMOS_" + MESES.get(mes - 1) + "_" + anio + ".xlsx";
    }
}
