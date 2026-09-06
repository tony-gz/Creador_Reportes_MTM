package com.simreports.core.plantilla;

/**
 * Tipo de tabla de la plantilla, según la clasificación de campos
 * lógicos resueltos — ver spec 01, sección 3 ("Resolver de tablas de la
 * plantilla") y spec 03 ("Reglas por tipo de tabla").
 */
public enum TipoTablaPlantilla {

    /**
     * Solo columna {@code LINEA} (sin {@code ICCID} ni {@code LINEA_2}).
     * Matching: {@code LINEA} contra Alestra.
     */
    SIMPLE_LINEA,

    /**
     * Solo columna {@code ICCID} (sin columnas de línea).
     * Matching: {@code ICCID} contra Telcel + ATT combinados.
     */
    SIMPLE_ICCID,

    /**
     * Columnas {@code LINEA} e {@code ICCID} a la vez (caso real más
     * común: 15 de las 18 hojas). Matching en cascada por fila: primero
     * {@code LINEA} contra Alestra, si no hay match, {@code ICCID}
     * contra Telcel + ATT — ver spec 03, regla 1.
     */
    SIMPLE_CASCADA,

    /**
     * Tabla dual SIM / router: {@code LINEA_1} + {@code LINEA_2} +
     * {@code CONSUMO(MB)_1} + {@code CONSUMO(MB)_2}. Solo 3 de las 18
     * hojas. {@code CONSUMO_TOTAL} se recalcula siempre, nunca se lee
     * como dato de entrada — ver spec 03, regla 4.
     */
    DUAL_SIM
}
