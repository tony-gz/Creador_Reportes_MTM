package com.simreports.web;

import com.simreports.PostgresEmbebido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La pantalla web (Fase 3, spec 11) la sirve la misma app. */
@SpringBootTest
@AutoConfigureMockMvc
class PaginaWebTest extends PostgresEmbebido {

    private static final Path ESTATICOS = Path.of("src/main/resources/static");

    @Autowired
    private MockMvc mvc;

    @Test
    void laRaizMuestraLaPagina() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reportes de consumo SIM")))
                .andExpect(content().string(containsString("js/app.js")));
    }

    @Test
    void sirveLosEstilosYTodosLosModulos() throws Exception {
        mvc.perform(get("/css/app.css")).andExpect(status().isOk());
        for (Path modulo : modulos()) {
            mvc.perform(get("/js/" + modulo.getFileName()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/javascript"));
        }
    }

    @Test
    void ningunModuloPintaHtmlConDatosDelServidor() throws IOException {
        // Spec 11: todo dato del servidor entra con textContent / nodos de texto.
        for (Path modulo : modulos()) {
            assertThat(Files.readString(modulo))
                    .as(modulo.getFileName().toString())
                    .doesNotContain("innerHTML", "outerHTML", "insertAdjacentHTML", "document.write");
        }
    }

    private static List<Path> modulos() throws IOException {
        try (Stream<Path> archivos = Files.list(ESTATICOS.resolve("js"))) {
            List<Path> lista = archivos.filter(p -> p.toString().endsWith(".js")).sorted().toList();
            assertThat(lista).isNotEmpty();
            return lista;
        }
    }
}
