package com.simreports.plantilla;

import com.simreports.PostgresEmbebido;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de la plantilla contra la app completa (Postgres embebido real). */
@SpringBootTest
@AutoConfigureMockMvc
class PlantillaControllerTest extends PostgresEmbebido {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void carpetaDeDatos(DynamicPropertyRegistry registro) {
        registro.add("simreports.almacenamiento.directorio", () -> datos.toString());
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlantillaConfigRepository repositorio;

    @AfterEach
    void limpiar() {
        repositorio.deleteAll();
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
            return new MockMultipartFile("archivo", "plantilla.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void subirUnaPlantillaValidaCreaLaVersionActivaYDevuelveSuMapeo() throws Exception {
        mvc.perform(multipart("/api/plantilla").file(plantilla("SIM", "LINEA", "ICCID", "CONSUMO(MB)")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version.version").value(1))
                .andExpect(jsonPath("$.version.activa").value(true))
                .andExpect(jsonPath("$.version.fechaCreacion").exists())
                .andExpect(jsonPath("$.mapeo.tablas", hasSize(1)))
                .andExpect(jsonPath("$.mapeo.tablas[0].hoja").value("GEPP"))
                .andExpect(jsonPath("$.mapeo.tablas[0].nombreTabla").value("Tabla3"))
                .andExpect(jsonPath("$.mapeo.tablas[0].tipo").value("SIMPLE_CASCADA"))
                .andExpect(jsonPath("$.mapeo.tablas[0].columnaPorCampoLogico.LINEA").value("LINEA"));
    }

    @Test
    void subirUnaPlantillaConProblemasDevuelve400ConLaListaDeProblemas() throws Exception {
        MockMultipartFile sinTablas;
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Row fila = libro.createSheet("DENTALIA").createRow(5);
            fila.createCell(0).setCellValue("LINEA");
            fila.createCell(1).setCellValue("CONSUMO(MB)");
            libro.write(out);
            sinTablas = new MockMultipartFile("archivo", "p.xlsx", "application/octet-stream", out.toByteArray());
        }

        mvc.perform(multipart("/api/plantilla").file(sinTablas))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Plantilla inválida"))
                .andExpect(jsonPath("$.problemas", hasSize(1)))
                .andExpect(jsonPath("$.problemas[0]", containsString("Hoja 'DENTALIA': no tiene ninguna tabla nativa")));
    }

    @Test
    void subirSinArchivoOConArchivoVacioDevuelve400() throws Exception {
        mvc.perform(multipart("/api/plantilla"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/plantilla").file(new MockMultipartFile("archivo", "v.xlsx", null, new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas[0]").value("El archivo está vacío."));
    }

    @Test
    void sinPlantillaActivaElMapeoDevuelve409() throws Exception {
        mvc.perform(get("/api/plantilla/mapeo"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Sin plantilla activa"));
    }

    @Test
    void devuelveElMapeoActivoYElHistorialDeVersiones() throws Exception {
        mvc.perform(multipart("/api/plantilla").file(plantilla("LINEA", "CONSUMO(MB)"))).andExpect(status().isCreated());
        mvc.perform(multipart("/api/plantilla").file(plantilla("ICCID", "CONSUMO(MB)"))).andExpect(status().isCreated());

        mvc.perform(get("/api/plantilla/mapeo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version.version").value(2))
                .andExpect(jsonPath("$.mapeo.tablas[0].tipo").value("SIMPLE_ICCID"));
        mvc.perform(get("/api/plantilla/versiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].version", contains(2, 1)))
                .andExpect(jsonPath("$[*].activa", contains(true, false)))
                .andExpect(jsonPath("$[0].rutaArchivo").doesNotExist());
    }

    @Test
    void corregirElMapeoReclasificaLaTablaSinCrearVersionNueva() throws Exception {
        mvc.perform(multipart("/api/plantilla").file(plantilla("Número", "ICCID", "CONSUMO(MB)")))
                .andExpect(jsonPath("$.mapeo.tablas[0].tipo").value("SIMPLE_ICCID"));

        mvc.perform(put("/api/plantilla/mapeo").contentType(MediaType.APPLICATION_JSON).content("""
                        {"hoja":"GEPP","nombreTabla":"Tabla3",
                         "columnaPorCampoLogico":{"LINEA":"Número","ICCID":"ICCID","CONSUMO(MB)":"CONSUMO(MB)"}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version.version").value(1))
                .andExpect(jsonPath("$.mapeo.tablas[0].tipo").value("SIMPLE_CASCADA"))
                .andExpect(jsonPath("$.mapeo.tablas[0].columnaPorCampoLogico.LINEA").value("Número"));

        // Persistido: se ve en el GET y sigue habiendo una sola versión.
        mvc.perform(get("/api/plantilla/mapeo"))
                .andExpect(jsonPath("$.mapeo.tablas[0].tipo").value("SIMPLE_CASCADA"));
        mvc.perform(get("/api/plantilla/versiones")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void unaCorreccionInvalidaDevuelve400YNoCambiaElMapeo() throws Exception {
        mvc.perform(multipart("/api/plantilla").file(plantilla("Número", "ICCID", "CONSUMO(MB)")));

        mvc.perform(put("/api/plantilla/mapeo").contentType(MediaType.APPLICATION_JSON).content("""
                        {"hoja":"GEPP","nombreTabla":"Tabla3",
                         "columnaPorCampoLogico":{"LINEA":"No existe","CONSUMO(MB)":"CONSUMO(MB)"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas[0]", containsString("no tiene la columna 'No existe'")));
        mvc.perform(put("/api/plantilla/mapeo").contentType(MediaType.APPLICATION_JSON).content("""
                        {"hoja":"GEPP","nombreTabla":"TablaX","columnaPorCampoLogico":{}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas[0]", containsString("no está en el mapeo de la plantilla activa")));
        mvc.perform(put("/api/plantilla/mapeo").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreTabla":"Tabla3","columnaPorCampoLogico":{}}"""))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/plantilla/mapeo"))
                .andExpect(jsonPath("$.mapeo.tablas[0].tipo").value("SIMPLE_ICCID"));
    }
}
