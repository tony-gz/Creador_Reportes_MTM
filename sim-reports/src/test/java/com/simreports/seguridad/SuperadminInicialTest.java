package com.simreports.seguridad;

import com.simreports.PostgresEmbebido;
import com.simreports.SesionesDePrueba;
import com.simreports.usuarios.Rol;
import com.simreports.usuarios.Usuario;
import com.simreports.usuarios.UsuarioRepository;
import com.simreports.usuarios.UsuarioService;
import com.simreports.web.SolicitudInvalidaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El primer superadmin sale de las variables de entorno (D-024). */
@SpringBootTest
class SuperadminInicialTest extends PostgresEmbebido {

    @Autowired
    private UsuarioService usuarios;
    @Autowired
    private UsuarioRepository repositorio;

    @BeforeEach
    @AfterEach
    void limpiar() {
        repositorio.deleteAll();
    }

    @Test
    void sinUsuariosCreaElSuperadminConContrasenaTemporal() {
        new SuperadminInicial(usuarios, " Tony ", "temporal-inicial-1", "Tony G").run(null);

        Usuario tony = usuarios.buscar("tony").orElseThrow();
        assertThat(tony.getRol()).isEqualTo(Rol.SUPERADMIN);
        assertThat(tony.getNombreCompleto()).isEqualTo("Tony G");
        assertThat(tony.isDebeCambiarPassword()).isTrue();
        assertThat(tony.getPasswordHash()).startsWith("$2").doesNotContain("temporal-inicial-1");
    }

    @Test
    void siYaHayUsuariosNoHaceNada() {
        SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);

        new SuperadminInicial(usuarios, "tony", "temporal-inicial-1", "Tony").run(null);

        assertThat(usuarios.buscar("tony")).isEmpty();
    }

    @Test
    void sinVariablesNoCreaNada() {
        new SuperadminInicial(usuarios, "", "", "Administrador").run(null);

        assertThat(repositorio.count()).isZero();
    }

    @Test
    void unaContrasenaDebilDetieneElArranque() {
        assertThatThrownBy(() -> new SuperadminInicial(usuarios, "tony", "corta", "Tony").run(null))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("al menos 10 caracteres");
    }
}
