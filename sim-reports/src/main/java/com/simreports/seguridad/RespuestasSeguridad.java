package com.simreports.seguridad;

import com.simreports.usuarios.SesionRespuesta;
import com.simreports.usuarios.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Respuestas de los filtros de seguridad, en el mismo formato RFC 9457 que el
 * resto de la API ({@code title}, {@code status}, {@code detail},
 * {@code problemas}).
 */
@Component
public class RespuestasSeguridad {

    static final String CREDENCIALES_INCORRECTAS = "Usuario o contraseña incorrectos.";
    static final String BLOQUEADO = "Demasiados intentos fallidos. Espera "
            + UsuarioService.BLOQUEO.toMinutes() + " minutos o pide al administrador que restablezca tu contraseña.";

    private final JsonMapper json;
    private final UsuarioService usuarios;

    public RespuestasSeguridad(JsonMapper json, UsuarioService usuarios) {
        this.json = Objects.requireNonNull(json, "json");
        this.usuarios = Objects.requireNonNull(usuarios, "usuarios");
    }

    /** Entró: reinicia los intentos y devuelve quién es. */
    void entradaCorrecta(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
        usuarios.registrarAcceso(auth.getName());
        SesionRespuesta sesion = usuarios.buscar(auth.getName()).map(SesionRespuesta::de).orElseThrow();
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(res.getOutputStream(), sesion);
    }

    /**
     * No entró. Usuario inexistente, desactivado o contraseña incorrecta dan el
     * mismo mensaje, para no revelar qué usuarios existen. El bloqueo sí se
     * dice: solo ocurre tras varios intentos.
     */
    void entradaFallida(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        String detalle = CREDENCIALES_INCORRECTAS;
        if (e instanceof BadCredentialsException) {
            String nombre = req.getParameter("usuario");
            usuarios.registrarFallo(nombre);
            boolean quedoBloqueado = usuarios.buscar(nombre)
                    .map(u -> u.bloqueadoEn(usuarios.horaActual())).orElse(false);
            if (quedoBloqueado) {
                detalle = BLOQUEADO;
            }
        } else if (e instanceof LockedException) {
            detalle = BLOQUEADO;
        }
        escribir(res, HttpServletResponse.SC_UNAUTHORIZED, "No se pudo entrar", detalle);
    }

    /** Sin sesión: la API responde 401; las páginas mandan a la pantalla de entrada. */
    void sinSesion(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        if (ruta(req).startsWith("/api/")) {
            escribir(res, HttpServletResponse.SC_UNAUTHORIZED, "Sin sesión", "Entra con tu usuario y contraseña.");
        } else {
            res.sendRedirect(req.getContextPath() + "/entrar.html");
        }
    }

    void accesoDenegado(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e) throws IOException {
        if (e instanceof CsrfException) {
            escribir(res, HttpServletResponse.SC_FORBIDDEN, "Sesión vencida", "Recarga la página e intenta de nuevo.");
        } else {
            escribir(res, HttpServletResponse.SC_FORBIDDEN, "Sin permiso", "Tu usuario no tiene permiso para esto.");
        }
    }

    void escribir(HttpServletResponse res, int estado, String titulo, String detalle) throws IOException {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "about:blank");
        cuerpo.put("title", titulo);
        cuerpo.put("status", estado);
        cuerpo.put("detail", detalle);
        cuerpo.put("problemas", List.of(detalle));
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(res.getOutputStream(), cuerpo);
    }

    static String ruta(HttpServletRequest req) {
        return req.getRequestURI().substring(req.getContextPath().length());
    }
}
