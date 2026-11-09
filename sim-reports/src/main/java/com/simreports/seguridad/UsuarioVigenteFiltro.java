package com.simreports.seguridad;

import com.simreports.usuarios.Usuario;
import com.simreports.usuarios.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * En cada llamada a la API con sesión, revisa al usuario en la base — D-024/D-026:
 * <ul>
 *   <li>si lo desactivaron, le cambiaron la contraseña o el rol, cierra la
 *       sesión (401);</li>
 *   <li>si tiene contraseña temporal, solo deja usar lo necesario para
 *       cambiarla (403 en lo demás).</li>
 * </ul>
 * No es un {@code @Component}: lo agrega {@link ConfiguracionSeguridad} a la
 * cadena de seguridad, para que no corra dos veces.
 */
public class UsuarioVigenteFiltro extends OncePerRequestFilter {

    /** Lo único permitido con contraseña temporal. */
    static final Set<String> CON_PASSWORD_TEMPORAL = Set.of("/api/sesion", "/api/sesion/password", "/api/sesion/csrf");

    private final UsuarioService usuarios;
    private final RespuestasSeguridad respuestas;

    public UsuarioVigenteFiltro(UsuarioService usuarios, RespuestasSeguridad respuestas) {
        this.usuarios = Objects.requireNonNull(usuarios, "usuarios");
        this.respuestas = Objects.requireNonNull(respuestas, "respuestas");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain cadena)
            throws ServletException, IOException {
        String ruta = RespuestasSeguridad.ruta(req);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (ruta.startsWith("/api/") && auth != null && auth.getPrincipal() instanceof UsuarioAutenticado sesion) {
            Optional<Usuario> actual = usuarios.buscar(sesion.getId());
            if (actual.isEmpty() || !vigente(actual.get(), sesion)) {
                HttpSession httpSession = req.getSession(false);
                if (httpSession != null) {
                    httpSession.invalidate();
                }
                SecurityContextHolder.clearContext();
                respuestas.escribir(res, HttpServletResponse.SC_UNAUTHORIZED, "Tu sesión terminó",
                        "Tu usuario cambió (contraseña, rol o estado). Vuelve a entrar.");
                return;
            }
            if (actual.get().isDebeCambiarPassword() && !CON_PASSWORD_TEMPORAL.contains(ruta)) {
                respuestas.escribir(res, HttpServletResponse.SC_FORBIDDEN, "Debes cambiar tu contraseña",
                        "Antes de seguir, cambia tu contraseña temporal.");
                return;
            }
        }
        cadena.doFilter(req, res);
    }

    private static boolean vigente(Usuario u, UsuarioAutenticado sesion) {
        return u.isActivo() && u.getRol() == sesion.getRol()
                && u.getVersionCredenciales() == sesion.getVersionCredenciales();
    }
}
