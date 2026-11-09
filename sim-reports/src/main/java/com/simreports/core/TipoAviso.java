package com.simreports.core;

/**
 * Tipos de aviso que puede generar un {@link OrigenParser} durante el
 * parseo de un archivo de origen (Alestra, Telcel, ATT) o el resolver de
 * la plantilla. Un aviso nunca detiene el proceso: solo se registra para
 * que la UI lo muestre al usuario (ver spec 02 y 03).
 */
public enum TipoAviso {

    /**
     * La clave (línea o ICCID) apareció más de una vez en el mismo
     * archivo/tabla. El comportamiento del sistema es sumar los consumos
     * (nunca quedarse con el primero o el último) — ver spec 02.
     */
    CLAVE_DUPLICADA,

    /**
     * La clave (línea o ICCID) llegó vacía o con un formato no utilizable
     * (por ejemplo, tras quitar espacios y comillas no queda ningún
     * carácter). La fila se excluye de la suma — ver spec 02, sección
     * "Caso borde: ICCID vacío o malformado".
     */
    CLAVE_VACIA_O_MALFORMADA,

    /**
     * Una fila de la plantilla dice un operador en su columna {@code SIM} /
     * {@code Carrier}, pero la SIM no está en ese operador y sí en el otro:
     * p. ej. dice ALESTRA, su línea no está en Alestra y su ICCID sí está en
     * Telcel/ATT. La fila queda en 0 (la app no cambia de operador) y el
     * aviso señala un probable error de captura — decisión D-016, spec 10.
     */
    CARRIER_NO_COINCIDE
}
