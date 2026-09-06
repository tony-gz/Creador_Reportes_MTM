package com.simreports.core;

import java.util.Objects;

/**
 * Un aviso no bloqueante generado durante el parseo de un archivo de
 * origen o la resolución de la plantilla (spec 02/03).
 *
 * @param tipo    qué tipo de situación se detectó.
 * @param clave   la línea o ICCID involucrada. Puede ser el valor crudo
 *                (sin normalizar) cuando el tipo es
 *                {@link TipoAviso#CLAVE_VACIA_O_MALFORMADA} y el valor
 *                original no estaba realmente vacío (por ejemplo, traía
 *                solo espacios o comillas) — así el usuario puede ver qué
 *                venía en el archivo. Nunca es {@code null}; si no hay
 *                valor crudo que mostrar, se usa cadena vacía.
 * @param detalle mensaje legible para mostrar en la UI, con contexto útil
 *                (p. ej. cuántas veces se repitió la clave, o de qué
 *                archivo/hoja viene).
 */
public record AvisoParseo(TipoAviso tipo, String clave, String detalle) {

    public AvisoParseo {
        Objects.requireNonNull(tipo, "tipo no puede ser null");
        Objects.requireNonNull(clave, "clave no puede ser null (usar cadena vacía si no aplica)");
        Objects.requireNonNull(detalle, "detalle no puede ser null");
    }
}
