package com.simreports.seguridad;

import com.simreports.usuarios.Rol;
import com.simreports.usuarios.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Crea el primer superadmin al arrancar — D-024 (spec 12). Solo si no existe
 * ningún usuario y están {@code SIMREPORTS_ADMIN_USUARIO} y
 * {@code SIMREPORTS_ADMIN_PASSWORD}. La contraseña es temporal: hay que
 * cambiarla al entrar. Después las variables se ignoran y se pueden quitar.
 */
@Component
public class SuperadminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperadminInicial.class);

    private final UsuarioService usuarios;
    private final String usuario;
    private final String password;
    private final String nombre;

    public SuperadminInicial(UsuarioService usuarios,
                             @Value("${simreports.admin.usuario:}") String usuario,
                             @Value("${simreports.admin.password:}") String password,
                             @Value("${simreports.admin.nombre:Administrador}") String nombre) {
        this.usuarios = Objects.requireNonNull(usuarios, "usuarios");
        this.usuario = usuario;
        this.password = password;
        this.nombre = nombre;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.hayUsuarios()) {
            if (!usuario.isBlank() || !password.isBlank()) {
                log.info("Ya hay usuarios: se ignoran SIMREPORTS_ADMIN_USUARIO y SIMREPORTS_ADMIN_PASSWORD (puedes quitarlas).");
            }
            return;
        }
        if (usuario.isBlank() || password.isBlank()) {
            log.warn("No hay usuarios. Define SIMREPORTS_ADMIN_USUARIO y SIMREPORTS_ADMIN_PASSWORD y reinicia para crear el superadmin.");
            return;
        }
        usuarios.crear(usuario, nombre, password, Rol.SUPERADMIN);
        log.info("Superadmin '{}' creado. Debe cambiar la contraseña la primera vez que entre.", UsuarioService.normalizar(usuario));
    }
}
