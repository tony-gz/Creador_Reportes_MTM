package com.simreports.seguridad;

import com.simreports.usuarios.CambioPasswordSolicitud;
import com.simreports.usuarios.SesionRespuesta;
import com.simreports.usuarios.Usuario;
import com.simreports.usuarios.UsuarioService;
import com.simreports.web.SolicitudInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

/**
 * La sesión propia — Fase 4 punto 2 (spec 12). Entrar y salir los atiende
 * Spring Security ({@link ConfiguracionSeguridad}).
 */
@RestController
@RequestMapping("/api/sesion")
public class SesionController {

    private final UsuarioService usuarios;
    private final SecurityContextRepository contextos = new HttpSessionSecurityContextRepository();

    public SesionController(UsuarioService usuarios) {
        this.usuarios = Objects.requireNonNull(usuarios, "usuarios");
    }

    /** Quién soy. 401 si no hay sesión. */
    @GetMapping
    public SesionRespuesta yo(@AuthenticationPrincipal UsuarioAutenticado sesion) {
        return usuarios.buscar(sesion.getId()).map(SesionRespuesta::de)
                .orElseThrow(() -> new SolicitudInvalidaException("El usuario ya no existe."));
    }

    /** Deja la cookie XSRF-TOKEN, para poder entrar. No necesita sesión. */
    @GetMapping("/csrf")
    public Map<String, Boolean> csrf(CsrfToken token) {
        token.getToken(); // al leerlo, el repositorio escribe la cookie
        return Map.of("listo", true);
    }

    /**
     * Cambia la contraseña propia. La sesión sigue abierta con los datos nuevos
     * (y un id de sesión nuevo); las demás sesiones de este usuario se cierran
     * solas en su siguiente llamada ({@link UsuarioVigenteFiltro}).
     */
    @PostMapping("/password")
    public SesionRespuesta cambiarPassword(@AuthenticationPrincipal UsuarioAutenticado sesion,
                                           @RequestBody CambioPasswordSolicitud solicitud,
                                           HttpServletRequest req, HttpServletResponse res) {
        Usuario usuario = usuarios.cambiarPasswordPropia(sesion.getId(), solicitud.actual(), solicitud.nueva());
        UsuarioAutenticado nuevo = UsuarioAutenticado.de(usuario, usuarios.horaActual());
        nuevo.eraseCredentials();
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(nuevo, null, nuevo.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        req.changeSessionId();
        contextos.saveContext(contexto, req, res);
        return SesionRespuesta.de(usuario);
    }
}
