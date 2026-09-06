package com.simreports.core;

/**
 * Se lanza cuando una hoja de la plantilla no tiene ninguna tabla nativa
 * de Excel ({@code XSSFSheet.getTables()} devuelve vacío). El resolver de
 * la plantilla no intenta adivinar un rango a partir de encabezados
 * sueltos — ver spec 01 y spec 00 ("Hoja de la plantilla sin ninguna
 * tabla nativa de Excel").
 */
public class HojaSinTablaException extends RuntimeException {

    private final String nombreHoja;

    public HojaSinTablaException(String nombreHoja) {
        super("La hoja '" + nombreHoja + "' no tiene ninguna tabla nativa de Excel (Table). "
                + "Conviértela a tabla nativa en la plantilla antes de generar el reporte.");
        this.nombreHoja = nombreHoja;
    }

    public String getNombreHoja() {
        return nombreHoja;
    }
}
