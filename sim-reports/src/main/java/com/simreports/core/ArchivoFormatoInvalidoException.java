package com.simreports.core;

/**
 * Se lanza cuando un archivo de origen (Alestra, Telcel, ATT) no tiene la
 * forma esperada: faltan campos lógicos obligatorios tras resolver
 * encabezados, no se encontró tabla nativa donde se esperaba (Alestra), o
 * no se encontró la fila de encabezado real dentro del límite de búsqueda
 * (Telcel .csv) — ver spec 02, sección "Validación de formato de
 * archivo".
 *
 * <p>Es unchecked a propósito: los parsers son código puro (sin conocer
 * HTTP ni Spring) y no deben forzar {@code throws} en toda la cadena de
 * llamadas. La traducción a una respuesta HTTP clara ocurre en la capa
 * web de Fase 2, vía un {@code @ControllerAdvice} (spec 04).</p>
 */
public class ArchivoFormatoInvalidoException extends RuntimeException {

    public ArchivoFormatoInvalidoException(String message) {
        super(message);
    }

    public ArchivoFormatoInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
