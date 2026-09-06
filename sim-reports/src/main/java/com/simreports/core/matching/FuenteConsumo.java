package com.simreports.core.matching;

/**
 * De qué fuente combinada vino un consumo encontrado durante el matching
 * — spec 03. Telcel y ATT se tratan como una sola fuente lógica porque
 * así se buscan (spec 03, regla 1: "ICCID contra Telcel + ATT
 * combinados") y así se agrupan en el reporte de consistencia de
 * totales ("para cada fuente F en {Alestra, Telcel+ATT}").
 */
public enum FuenteConsumo {
    ALESTRA,
    TELCEL_ATT
}