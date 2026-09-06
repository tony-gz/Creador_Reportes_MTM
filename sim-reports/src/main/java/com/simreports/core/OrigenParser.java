package com.simreports.core;

import java.io.InputStream;

/**
 * Contrato común de los tres parsers de origen (Alestra, Telcel, ATT) —
 * ver spec 02, sección "Interfaz común". Cada implementación conoce el
 * formato específico de su operador (xlsx con tabla nativa, csv con
 * metadata previa, etc.) pero todas exponen el mismo método.
 *
 * <p>Puro: no conoce HTTP ni Spring. Ante un archivo con forma inesperada
 * lanza {@link ArchivoFormatoInvalidoException} (unchecked) en vez de
 * devolver un resultado parcial o silencioso.</p>
 */
public interface OrigenParser {

    /**
     * Parsea el archivo de origen y devuelve el consumo ya agregado por
     * clave (línea o ICCID), junto con los avisos no bloqueantes
     * detectados.
     *
     * @param archivo contenido crudo del archivo subido por el usuario.
     *                El nombre de archivo nunca debe usarse para decidir
     *                el formato (spec 02: "nombre de archivo arbitrario").
     * @throws ArchivoFormatoInvalidoException si, tras resolver
     *         encabezados, faltan campos lógicos obligatorios, o no se
     *         encuentra la fila/tabla de encabezado esperada.
     */
    ResultadoParseo parsear(InputStream archivo);
}
