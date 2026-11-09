package com.simreports.seguridad;

import com.simreports.PostgresEmbebido;
import com.simreports.SesionesDePrueba;
import com.simreports.usuarios.Rol;
import com.simreports.usuarios.UsuarioRepository;
import com.simreports.usuarios.UsuarioService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El flujo CSRF real de la pantalla: pedir la cookie XSRF-TOKEN y mandarla en
 * la cabecera X-XSRF-TOKEN. Contexto propio y sin el ayudante {@code csrf()}
 * de spring-security-test, que reemplaza el repositorio de tokens del contexto.
 */
@SpringBootTest(properties = "simreports.test.contexto=csrf-real")
@AutoConfigureMockMvc
class CsrfRealTest extends PostgresEmbebido {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioService usuarios;
    @Autowired
    private UsuarioRepository repositorio;

    @AfterEach
    void limpiar() {
        repositorio.deleteAll();
    }

    @Test
    void entraConLaCookieYLaCabeceraCsrf() throws Exception {
        SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);

        Cookie token = mvc.perform(get("/api/sesion/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/sesion/entrar").cookie(token).header("X-XSRF-TOKEN", token.getValue())
                        .param("usuario", "ana").param("password", SesionesDePrueba.DEFINITIVA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("ana"));
    }

    @Test
    void sinLaCabeceraOConOtroValorSeRechaza() throws Exception {
        SesionesDePrueba.usuarioListo(usuarios, "ana", Rol.USUARIO);
        Cookie token = mvc.perform(get("/api/sesion/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/sesion/entrar").cookie(token)
                        .param("usuario", "ana").param("password", SesionesDePrueba.DEFINITIVA))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/sesion/entrar").cookie(token).header("X-XSRF-TOKEN", "otro-valor")
                        .param("usuario", "ana").param("password", SesionesDePrueba.DEFINITIVA))
                .andExpect(status().isForbidden());
    }
}
