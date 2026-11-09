package com.simreports.usuarios;

/** Roles de la app — D-024 (spec 12). */
public enum Rol {
    /** Administra usuarios y ve la actividad; también genera reportes. */
    SUPERADMIN,
    /** Genera reportes y cambia su propia contraseña. */
    USUARIO
}
