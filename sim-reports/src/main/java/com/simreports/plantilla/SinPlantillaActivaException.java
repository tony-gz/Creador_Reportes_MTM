package com.simreports.plantilla;

/** Se pidió la plantilla activa y todavía no se ha subido ninguna. */
public class SinPlantillaActivaException extends RuntimeException {

    public SinPlantillaActivaException() {
        super("Todavía no hay una plantilla activa. Sube la plantilla antes de generar un reporte.");
    }
}
