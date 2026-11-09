package com.simreports.seguridad;

import com.simreports.usuarios.UsuarioService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Carga el usuario para Spring Security; también evita el usuario en memoria por defecto de Spring Boot. */
@Service
public class DetallesUsuarioService implements UserDetailsService {

    private final UsuarioService usuarios;

    public DetallesUsuarioService(UsuarioService usuarios) {
        this.usuarios = Objects.requireNonNull(usuarios, "usuarios");
    }

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) {
        return usuarios.buscar(nombreUsuario)
                .map(u -> UsuarioAutenticado.de(u, usuarios.horaActual()))
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario"));
    }
}
