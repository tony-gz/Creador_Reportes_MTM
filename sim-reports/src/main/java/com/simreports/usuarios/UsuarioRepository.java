package com.simreports.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** {@code nombreUsuario} ya en minúsculas ({@link UsuarioService#normalizar}). */
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    List<Usuario> findAllByOrderByNombreUsuarioAsc();

    long countByRolAndActivoTrue(Rol rol);
}
