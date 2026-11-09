package com.simreports.web;

import java.util.List;

/** Datos de una solicitud que no cumplen las reglas: 400 con todos los problemas. */
public class SolicitudInvalidaException extends RuntimeException {

    private final List<String> problemas;

    public SolicitudInvalidaException(List<String> problemas) {
        super(String.join(" ", problemas));
        this.problemas = List.copyOf(problemas);
    }

    public SolicitudInvalidaException(String problema) {
        this(List.of(problema));
    }

    public List<String> getProblemas() {
        return problemas;
    }
}
