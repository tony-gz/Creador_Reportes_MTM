package com.simreports.core.plantilla;

/**
 * Se lanza cuando una tabla de la plantilla no se puede clasificar de
 * forma segura en ninguno de los cuatro tipos conocidos ({@link
 * TipoTablaPlantilla}) — por ejemplo, le faltan columnas para completar
 * un tipo dual SIM, o no tiene columna de consumo donde escribir el
 * resultado.
 *
 * <p>Es unchecked a propósito, igual que el resto de excepciones del
 * motor de parsing puro (ver {@code ArchivoFormatoInvalidoException} y
 * {@code HojaSinTablaException} en {@code com.simreports.core}): se
 * captura explícitamente en la capa web de Fase 2. Se prefiere fallar
 * ruidosamente aquí — con el detalle de qué encabezados se encontraron —
 * en vez de adivinar una clasificación que podría ser incorrecta y pasar
 * desapercibida (misma filosofía que {@code HojaSinTablaException},
 * spec 01).</p>
 */
public class TablaPlantillaNoReconocidaException extends RuntimeException {

    public TablaPlantillaNoReconocidaException(String message) {
        super(message);
    }
}
