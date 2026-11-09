package com.simreports.usuarios;

/** Quién tiene la sesión abierta, para la pantalla. */
public record SesionRespuesta(String usuario, String nombreCompleto, Rol rol, boolean debeCambiarPassword) {

    public static SesionRespuesta de(Usuario u) {
        return new SesionRespuesta(u.getNombreUsuario(), u.getNombreCompleto(), u.getRol(), u.isDebeCambiarPassword());
    }
}
