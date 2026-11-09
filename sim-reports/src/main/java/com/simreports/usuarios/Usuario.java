package com.simreports.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Usuario de la app — D-024 y D-026 (spec 12). La contraseña solo se guarda
 * como hash BCrypt; el nombre de usuario, en minúsculas.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_usuario", nullable = false, length = 50)
    private String nombreUsuario;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password_actualizado", nullable = false)
    private Instant passwordActualizado;

    /** Sube con cada cambio de contraseña; las sesiones con otra versión se cierran. */
    @Column(name = "version_credenciales", nullable = false)
    private int versionCredenciales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    protected Usuario() {
        // JPA
    }

    /** Usuario nuevo, activo, que debe cambiar la contraseña al entrar. */
    Usuario(String nombreUsuario, String nombreCompleto, String passwordHash, Rol rol, Instant ahora) {
        this.nombreUsuario = Objects.requireNonNull(nombreUsuario, "nombreUsuario");
        this.nombreCompleto = Objects.requireNonNull(nombreCompleto, "nombreCompleto");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.rol = Objects.requireNonNull(rol, "rol");
        this.passwordActualizado = ahora;
        this.fechaCreacion = ahora;
        this.activo = true;
        this.debeCambiarPassword = true;
    }

    void cambiarPassword(String nuevoHash, boolean temporal, Instant ahora) {
        this.passwordHash = Objects.requireNonNull(nuevoHash, "nuevoHash");
        this.passwordActualizado = ahora;
        this.versionCredenciales++;
        this.debeCambiarPassword = temporal;
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    /** Un intento fallido; al llegar a {@code maximo} queda bloqueado hasta {@code hasta}. */
    void registrarFallo(int maximo, Instant hasta) {
        intentosFallidos++;
        if (intentosFallidos >= maximo) {
            bloqueadoHasta = hasta;
            intentosFallidos = 0;
        }
    }

    void registrarAcceso(Instant ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoAcceso = ahora;
    }

    void setActivo(boolean activo) {
        this.activo = activo;
    }

    void setRol(Rol rol) {
        this.rol = Objects.requireNonNull(rol, "rol");
    }

    void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = Objects.requireNonNull(nombreCompleto, "nombreCompleto");
    }

    public boolean bloqueadoEn(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public Long getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getPasswordActualizado() {
        return passwordActualizado;
    }

    public int getVersionCredenciales() {
        return versionCredenciales;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getUltimoAcceso() {
        return ultimoAcceso;
    }
}
