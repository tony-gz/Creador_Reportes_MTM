package com.simreports.usuarios;

import com.simreports.web.SolicitudInvalidaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Usuarios y contraseñas — D-024 y D-026 (spec 12).
 *
 * <ul>
 *   <li>Nombre de usuario: 3 a 50 caracteres {@code a-z 0-9 . _ -}, se guarda
 *       en minúsculas.</li>
 *   <li>Contraseñas con BCrypt ({@link PoliticaPassword}). Las que asigna
 *       otra persona (alta, restablecer) son temporales: hay que cambiarlas al
 *       entrar.</li>
 *   <li>{@value #MAXIMO_INTENTOS} intentos fallidos seguidos bloquean el
 *       usuario {@link #BLOQUEO}.</li>
 * </ul>
 */
@Service
public class UsuarioService {

    public static final int MAXIMO_INTENTOS = 5;
    public static final Duration BLOQUEO = Duration.ofMinutes(15);
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[a-z0-9._-]{3,50}");

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final Clock reloj;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder encoder, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.encoder = Objects.requireNonNull(encoder, "encoder");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    public static String normalizar(String nombreUsuario) {
        return nombreUsuario == null ? "" : nombreUsuario.strip().toLowerCase(Locale.ROOT);
    }

    /** Postgres guarda microsegundos: así lo que queda en memoria es igual a lo que se relee. */
    Instant ahora() {
        return Instant.now(reloj).truncatedTo(ChronoUnit.MICROS);
    }

    /** Alta con contraseña temporal. */
    @Transactional
    public Usuario crear(String nombreUsuario, String nombreCompleto, String passwordTemporal, Rol rol) {
        String nombre = normalizar(nombreUsuario);
        String completo = nombreCompleto == null ? "" : nombreCompleto.strip();
        List<String> problemas = new ArrayList<>();
        if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
            problemas.add("El nombre de usuario debe tener de 3 a 50 caracteres: letras sin acentos, números, punto, guion o guion bajo.");
        } else if (repositorio.findByNombreUsuario(nombre).isPresent()) {
            problemas.add("Ya existe el usuario '" + nombre + "'.");
        }
        if (completo.isEmpty() || completo.length() > 100) {
            problemas.add("El nombre completo es obligatorio (máximo 100 caracteres).");
        }
        if (rol == null) {
            problemas.add("Falta el rol.");
        }
        problemas.addAll(PoliticaPassword.problemas(passwordTemporal, nombre));
        if (!problemas.isEmpty()) {
            throw new SolicitudInvalidaException(problemas);
        }
        return repositorio.save(new Usuario(nombre, completo, encoder.encode(passwordTemporal), rol, ahora()));
    }

    /** El propio usuario cambia su contraseña; deja de ser temporal. */
    @Transactional
    public Usuario cambiarPasswordPropia(Long id, String actual, String nueva) {
        Usuario usuario = repositorio.findById(id)
                .orElseThrow(() -> new SolicitudInvalidaException("El usuario ya no existe."));
        if (actual == null || !encoder.matches(actual, usuario.getPasswordHash())) {
            throw new SolicitudInvalidaException("La contraseña actual no es correcta.");
        }
        List<String> problemas = new ArrayList<>(PoliticaPassword.problemas(nueva, usuario.getNombreUsuario()));
        if (problemas.isEmpty() && encoder.matches(nueva, usuario.getPasswordHash())) {
            problemas.add("La contraseña nueva debe ser distinta de la actual.");
        }
        if (!problemas.isEmpty()) {
            throw new SolicitudInvalidaException(problemas);
        }
        usuario.cambiarPassword(encoder.encode(nueva), false, ahora());
        return usuario;
    }

    /** Contraseña incorrecta: cuenta el intento y bloquea al llegar al máximo. */
    @Transactional
    public void registrarFallo(String nombreUsuario) {
        repositorio.findByNombreUsuario(normalizar(nombreUsuario))
                .ifPresent(u -> u.registrarFallo(MAXIMO_INTENTOS, ahora().plus(BLOQUEO)));
    }

    /** Entrada correcta: reinicia los intentos y guarda la hora. */
    @Transactional
    public void registrarAcceso(String nombreUsuario) {
        repositorio.findByNombreUsuario(normalizar(nombreUsuario)).ifPresent(u -> u.registrarAcceso(ahora()));
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscar(Long id) {
        return repositorio.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscar(String nombreUsuario) {
        return repositorio.findByNombreUsuario(normalizar(nombreUsuario));
    }

    @Transactional(readOnly = true)
    public boolean hayUsuarios() {
        return repositorio.count() > 0;
    }

    public Instant horaActual() {
        return ahora();
    }
}
