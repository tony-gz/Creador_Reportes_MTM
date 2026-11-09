package com.simreports.plantilla;

import com.simreports.PostgresEmbebido;
import com.simreports.SesionesDePrueba;
import com.simreports.usuarios.Rol;
import com.simreports.usuarios.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revisar la plantilla sin guardarla (D-023), contra la app completa. */
@SpringBootTest
@AutoConfigureMockMvc
class PlantillaControllerTest extends PostgresEmbebido {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioService usuarios;

    private RequestPostProcessor sesion;

    @BeforeEach
    void entrar() {
        sesion = SesionesDePrueba.sesion(SesionesDePrueba.usuarioListo(usuarios, "plantilla.prueba", Rol.USUARIO));
    }

    /** Hoja GEPP con una tabla: encabezados dados, una fila de datos. */
    private static MockMultipartFile plantilla(String... encabezados) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet hoja = libro.createSheet("GEPP");
            Row fila0 = hoja.createRow(0);
            Row fila1 = hoja.createRow(1);
            for (int i = 0; i < encabezados.length; i++) {
                fila0.createCell(i).setCellValue(encabezados[i]);
                fila1.createCell(i).setCellValue(encabezados[i].startsWith("CONSUMO") ? "0" : "5511112222");
            }
            XSSFTable tabla = hoja.createTable(new AreaReference(new CellReference(0, 0),
                    new CellReference(1, encabezados.length - 1), SpreadsheetVersion.EXCEL2007));
            tabla.setName("Tabla3");
            tabla.setDisplayName("Tabla3");
            tabla.updateHeaders();
            libro.write(out);
            return new MockMultipartFile("plantilla", "plantilla.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void revisarDevuelveElMapeoDeLaPlantilla() throws Exception {
        mvc.perform(multipart("/api/plantilla/revisar").file(plantilla("SIM", "LINEA", "ICCID", "CONSUMO(MB)")).with(sesion).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tablas", hasSize(1)))
                .andExpect(jsonPath("$.tablas[0].hoja").value("GEPP"))
                .andExpect(jsonPath("$.tablas[0].nombreTabla").value("Tabla3"))
                .andExpect(jsonPath("$.tablas[0].tipo").value("SIMPLE_CASCADA"))
                .andExpect(jsonPath("$.tablas[0].columnaPorCampoLogico.LINEA").value("LINEA"));
    }

    @Test
    void unaPlantillaConProblemasDevuelve400ConLaListaDeProblemas() throws Exception {
        MockMultipartFile sinTablas;
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Row fila = libro.createSheet("DENTALIA").createRow(5);
            fila.createCell(0).setCellValue("LINEA");
            fila.createCell(1).setCellValue("CONSUMO(MB)");
            libro.write(out);
            sinTablas = new MockMultipartFile("plantilla", "p.xlsx", "application/octet-stream", out.toByteArray());
        }

        mvc.perform(multipart("/api/plantilla/revisar").file(sinTablas).with(sesion).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Plantilla inválida"))
                .andExpect(jsonPath("$.problemas", hasSize(1)))
                .andExpect(jsonPath("$.problemas[0]", containsString("Hoja 'DENTALIA': no tiene ninguna tabla nativa")));
    }

    @Test
    void sinArchivoOConArchivoVacioDevuelve400() throws Exception {
        mvc.perform(multipart("/api/plantilla/revisar").with(sesion).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/plantilla/revisar").file(new MockMultipartFile("plantilla", "v.xlsx", null, new byte[0])).with(sesion).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas[0]").value("El archivo de la plantilla está vacío."));
    }
}
