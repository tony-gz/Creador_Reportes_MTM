package com.simreports.reportes;

import com.simreports.PostgresEmbebido;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Generar un reporte sin guardar nada (D-023), contra la app completa. */
@SpringBootTest
@AutoConfigureMockMvc
class ReporteControllerTest extends PostgresEmbebido {

    @Autowired
    private MockMvc mvc;

    private final JsonMapper json = JsonMapper.builder().build();

    private static MockMultipartHttpServletRequestBuilder solicitud(String anio, String mes, byte[] plantilla, byte[] telcel) {
        MockMultipartHttpServletRequestBuilder s = multipart("/api/reportes")
                .file(new MockMultipartFile("plantilla", "CONSUMOS_SEPTIEMBRE_2026.xlsx", null, plantilla))
                .file(new MockMultipartFile("alestra", "Tráfico por línea.xlsx", null, ArchivosDePrueba.alestra()))
                .file(new MockMultipartFile("telcel", "Uso_de_datos.csv", "text/csv", telcel))
                .file(new MockMultipartFile("att", "att.csv", "text/csv", ArchivosDePrueba.att()));
        s.param("anio", anio).param("mes", mes);
        return s;
    }

    private static MockMultipartHttpServletRequestBuilder solicitud(String mes) {
        return solicitud("2026", mes, ArchivosDePrueba.plantilla(), ArchivosDePrueba.telcel());
    }

    @Test
    void generarDevuelveElDetalleYElReporteFinal() throws Exception {
        mvc.perform(solicitud("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreArchivo").value("CONSUMOS_OCTUBRE_2026.xlsx"))
                .andExpect(jsonPath("$.periodoAnio").value(2026))
                .andExpect(jsonPath("$.periodoMes").value(10))
                .andExpect(jsonPath("$.fecha").exists())
                .andExpect(jsonPath("$.reporteFinal", startsWith("REPORTE FINAL DE LA CORRIDA")))
                .andExpect(jsonPath("$.archivosEntrada[*].fuente", contains("PLANTILLA", "ALESTRA", "TELCEL", "ATT")))
                .andExpect(jsonPath("$.archivosEntrada[1].nombre").value("Tráfico por línea.xlsx"))
                .andExpect(jsonPath("$.consumoSinCliente[?(@.fuente == 'ALESTRA')].clave",
                        contains(ArchivosDePrueba.LINEA_SIN_CLIENTE)))
                .andExpect(jsonPath("$.avisos.totales", hasSize(2)))
                .andExpect(jsonPath("$.avisos.simsRepetidas", hasSize(0)));
    }

    @Test
    void laRespuestaTraeElXlsxEnBase64() throws Exception {
        String cuerpo = mvc.perform(solicitud("10")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        byte[] archivo = Base64.getDecoder().decode(json.readTree(cuerpo).get("xlsxBase64").asString());

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            assertThat(libro.getSheet("GEPP").getRow(1).getCell(3).getNumericCellValue()).isEqualTo(100.5);
            assertThat(libro.getSheet("BD_ALESTRA")).isNull();
        }
    }

    @Test
    void erroresDeLaSolicitudDevuelven400ConProblemas() throws Exception {
        mvc.perform(solicitud("13"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.problemas[0]", containsString("El mes debe estar entre 1 y 12")));
        mvc.perform(solicitud("2026", "10", ArchivosDePrueba.plantilla(), ArchivosDePrueba.telcelTablaDinamica()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Archivo con formato inválido"))
                .andExpect(jsonPath("$.problemas[0]", startsWith("Archivo de Telcel ('Uso_de_datos.csv'): ")));
        mvc.perform(solicitud("2026", "10", ArchivosDePrueba.plantilla(), new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas[0]").value("El archivo de Telcel está vacío."));
        mvc.perform(solicitud("2026", "10", new byte[0], ArchivosDePrueba.telcel()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas[0]").value("El archivo de la plantilla está vacío."));
        mvc.perform(multipart("/api/reportes").param("anio", "2026").param("mes", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas", hasSize(1)));
    }

    @Test
    void unaPlantillaSinTablasDevuelve400ConSusProblemas() throws Exception {
        byte[] sinTablas;
        try (XSSFWorkbook libro = new XSSFWorkbook(); var out = new java.io.ByteArrayOutputStream()) {
            libro.createSheet("DENTALIA").createRow(5).createCell(0).setCellValue("LINEA");
            libro.write(out);
            sinTablas = out.toByteArray();
        }
        mvc.perform(solicitud("2026", "10", sinTablas, ArchivosDePrueba.telcel()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Plantilla inválida"));
    }
}
