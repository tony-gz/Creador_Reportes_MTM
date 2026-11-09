package com.simreports.seguridad;

import com.simreports.PostgresEmbebido;
import com.simreports.RelojDePrueba;
import com.simreports.SesionesDePrueba;
import com.simreports.usuarios.Rol;
import com.simreports.usuarios.Usuario;
import com.simreports.usuarios.UsuarioRepository;
import com.simreports.usuarios.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Inicio de sesión, bloqueo, contraseña temporal y permisos — Fase 4 punto 2 (spec 12). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SeguridadTest.Reloj.class)
class SeguridadTest extends PostgresEmbebido {

    @TestConfiguration
    static class Reloj {
        @Bean
        @Primary
        RelojDePrueba relojDePrueba() {
            return new RelojDePrueba();
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioService usuarios;
    @Autowired
    private UsuarioRepository repositorio;
    @Autowired
    private RelojDePrueba reloj;
    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void limpiar() {
        repositorio.deleteAll();
        reloj.reiniciar();
    }

    private ResultActions entrar(String usuario, String password) throws Exception {
        return mvc.perform(post("/api/sesion/entrar").param("usuario", usuario).param("password", password).with(csrf()));
    }

    private MockHttpSession sesionDe(String usuario, String password) throws Exception {
        return (MockHttpSession) entrar(usuario, password).andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    // ---------- Sin sesión ----------

    @Test
    void sinSesionLaApiResponde401EnJson() throws Exception {
        mvc.perform(get("/api/sesion"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Sin sesión"))
                .andExpect(jsonPath("$.problemas[0]").value("Entra con tu usuario y contraseña."));
        mvc.perform(multipart("/api/reportes").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void entrarSinTokenCsrfSeRechaza() throws Exception {
        usuarios.crear("ana", "Ana", SesionesDePrueba.TEMPORAL, Rol.USUARIO);

        mvc.perform(post("/api/sesion/entrar").param("usuario", "ana").param("password", SesionesDePrueba.TEMPORAL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sesión vencida"));
    }

    // ---------- Entrar ----------

    @Test
    void entrarDevuelveQuienEsYAbreLaSesion() throws Exception {
        SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);

        MockHttpSession sesion = (MockHttpSession) entrar("  ANA ", SesionesDePrueba.DEFINITIVA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("ana"))
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andExpect(jsonPath("$.debeCambiarPassword").value(false))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/sesion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Prueba ana"));
        assertThat(usuarios.buscar("ana").orElseThrow().getUltimoAcceso()).isNotNull();
    }

    @Test
    void usuarioInexistenteYContrasenaIncorrectaDanElMismoMensaje() throws Exception {
        SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);

        entrar("ana", "otra-cosa-123").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(RespuestasSeguridad.CREDENCIALES_INCORRECTAS));
        entrar("nadie", "otra-cosa-123").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(RespuestasSeguridad.CREDENCIALES_INCORRECTAS));
    }

    @Test
    void cincoIntentosFallidosBloqueanQuinceMinutos() throws Exception {
        SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);

        for (int i = 1; i < UsuarioService.MAXIMO_INTENTOS; i++) {
            entrar("ana", "incorrecta-" + i).andExpect(jsonPath("$.detail").value(RespuestasSeguridad.CREDENCIALES_INCORRECTAS));
        }
        entrar("ana", "incorrecta-5").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(RespuestasSeguridad.BLOQUEADO));
        // Ni con la contraseña correcta
        entrar("ana", SesionesDePrueba.DEFINITIVA).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(RespuestasSeguridad.BLOQUEADO));

        reloj.adelantar(UsuarioService.BLOQUEO.plusSeconds(1));
        entrar("ana", SesionesDePrueba.DEFINITIVA).andExpect(status().isOk());
    }

    @Test
    void unUsuarioDesactivadoNoEntra() throws Exception {
        Usuario ana = SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);
        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", ana.getId());

        entrar("ana", SesionesDePrueba.DEFINITIVA).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(RespuestasSeguridad.CREDENCIALES_INCORRECTAS));
    }

    @Test
    void desactivarAUnUsuarioCierraSuSesionAbierta() throws Exception {
        Usuario ana = SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);
        MockHttpSession sesion = sesionDe("ana", SesionesDePrueba.DEFINITIVA);

        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", ana.getId());

        mvc.perform(get("/api/sesion").session(sesion))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Tu sesión terminó"));
        assertThat(sesion.isInvalid()).isTrue();
    }

    // ---------- Contraseña temporal ----------

    @Test
    void conContrasenaTemporalSoloSePuedeCambiarla() throws Exception {
        usuarios.crear("ana", "Ana", SesionesDePrueba.TEMPORAL, Rol.USUARIO);
        MockHttpSession sesion = (MockHttpSession) entrar("ana", SesionesDePrueba.TEMPORAL)
                .andExpect(jsonPath("$.debeCambiarPassword").value(true))
                .andReturn().getRequest().getSession(false);

        mvc.perform(multipart("/api/plantilla/revisar").session(sesion).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Debes cambiar tu contraseña"));

        cambiar(sesion, "equivocada-123", "nueva-segura-2026").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas[0]").value("La contraseña actual no es correcta."));
        cambiar(sesion, SesionesDePrueba.TEMPORAL, "corta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas", hasItem(containsString("al menos 10 caracteres"))));
        cambiar(sesion, SesionesDePrueba.TEMPORAL, SesionesDePrueba.TEMPORAL).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas[0]").value("La contraseña nueva debe ser distinta de la actual."));

        cambiar(sesion, SesionesDePrueba.TEMPORAL, "nueva-segura-2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(false));

        // La misma sesión sigue abierta y ya puede usar la app (400: falta el archivo, no 403).
        mvc.perform(multipart("/api/plantilla/revisar").session(sesion).with(csrf()))
                .andExpect(status().isBadRequest());
        entrar("ana", "nueva-segura-2026").andExpect(status().isOk());
    }

    @Test
    void cambiarLaContrasenaCierraLasOtrasSesiones() throws Exception {
        SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);
        MockHttpSession enLaOficina = sesionDe("ana", SesionesDePrueba.DEFINITIVA);
        MockHttpSession enCasa = sesionDe("ana", SesionesDePrueba.DEFINITIVA);

        cambiar(enCasa, SesionesDePrueba.DEFINITIVA, "otra-definitiva-2026").andExpect(status().isOk());

        mvc.perform(get("/api/sesion").session(enCasa)).andExpect(status().isOk());
        mvc.perform(get("/api/sesion").session(enLaOficina)).andExpect(status().isUnauthorized());
    }

    private ResultActions cambiar(MockHttpSession sesion, String actual, String nueva) throws Exception {
        return mvc.perform(post("/api/sesion/password").session(sesion).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"" + actual + "\",\"nueva\":\"" + nueva + "\"}"));
    }

    // ---------- Salir y permisos ----------

    @Test
    void salirCierraLaSesion() throws Exception {
        SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);
        MockHttpSession sesion = sesionDe("ana", SesionesDePrueba.DEFINITIVA);

        mvc.perform(post("/api/sesion/salir").session(sesion).with(csrf())).andExpect(status().isNoContent());

        mvc.perform(get("/api/sesion").session(sesion)).andExpect(status().isUnauthorized());
    }

    @Test
    void laAdministracionEsSoloDelSuperadmin() throws Exception {
        Usuario ana = SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);

        mvc.perform(get("/api/admin/usuarios").with(SesionesDePrueba.sesion(ana)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sin permiso"));
    }

    @Test
    void lasRespuestasTraenLaPoliticaDeContenido() throws Exception {
        mvc.perform(get("/entrar.html"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
    }

    @Test
    void elUsuarioYaNoDependeDelRelojDelSistema() {
        // El bloqueo se calcula con el reloj inyectado (sanidad del montaje del test).
        Duration d = Duration.ofHours(1);
        reloj.adelantar(d);
        assertThat(usuarios.horaActual()).isAfter(java.time.Instant.now().plus(Duration.ofMinutes(59)));
    }
}
