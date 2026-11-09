package com.simreports.seguridad;

import com.simreports.usuarios.Rol;
import com.simreports.usuarios.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * La persona que entró, tal como queda en su sesión. {@code versionCredenciales}
 * y {@code rol} sirven para cerrar la sesión si cambian ({@link UsuarioVigenteFiltro}).
 * El hash se borra después de entrar ({@link CredentialsContainer}).
 */
public final class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String nombreUsuario;
    private final String nombreCompleto;
    private final Rol rol;
    private final int versionCredenciales;
    private final boolean activo;
    private final boolean bloqueado;
    private String passwordHash;

    private UsuarioAutenticado(Usuario u, boolean bloqueado) {
        this.id = Objects.requireNonNull(u.getId(), "id");
        this.nombreUsuario = u.getNombreUsuario();
        this.nombreCompleto = u.getNombreCompleto();
        this.rol = u.getRol();
        this.versionCredenciales = u.getVersionCredenciales();
        this.activo = u.isActivo();
        this.bloqueado = bloqueado;
        this.passwordHash = u.getPasswordHash();
    }

    public static UsuarioAutenticado de(Usuario u, Instant ahora) {
        return new UsuarioAutenticado(u, u.bloqueadoEn(ahora));
    }

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public Rol getRol() {
        return rol;
    }

    public int getVersionCredenciales() {
        return versionCredenciales;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return nombreUsuario;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
