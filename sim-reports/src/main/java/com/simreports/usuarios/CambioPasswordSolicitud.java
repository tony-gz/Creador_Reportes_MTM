package com.simreports.usuarios;

/** Cuerpo de {@code POST /api/sesion/password}. */
public record CambioPasswordSolicitud(String actual, String nueva) {
}
