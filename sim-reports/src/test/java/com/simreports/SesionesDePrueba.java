package com.simreports;

import com.simreports.seguridad.UsuarioAutenticado;
import com.simreports.usuarios.Rol;
import com.simreports.usuarios.Usuario;
import com.simreports.usuarios.UsuarioService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/** Usuarios y sesiones para los tests que llaman a la API (Fase 4, spec 12). */
public final class SesionesDePrueba {

    public static final String TEMPORAL = "temporal-de-prueba-1";
    public static final String DEFINITIVA = "definitiva-de-prueba-1";

    private SesionesDePrueba() {
    }

    /** Usuario activo con contraseña definitiva ({@link #DEFINITIVA}); lo crea si no existe. */
    public static Usuario usuarioListo(UsuarioService usuarios, String nombre, Rol rol) {
        Usuario u = usuarios.buscar(nombre).orElseGet(() -> usuarios.crear(nombre, "Prueba " + nombre, TEMPORAL, rol));
        if (u.isDebeCambiarPassword()) {
            u = usuarios.cambiarPasswordPropia(u.getId(), TEMPORAL, DEFINITIVA);
        }
        return u;
    }

    /** Sesión abierta como {@code u}, para MockMvc. */
    public static RequestPostProcessor sesion(Usuario u) {
        UsuarioAutenticado principal = UsuarioAutenticado.de(u, Instant.now());
        principal.eraseCredentials();
        return authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }
}
