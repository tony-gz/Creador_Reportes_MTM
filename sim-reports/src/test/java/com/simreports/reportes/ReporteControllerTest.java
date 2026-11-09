package com.simreports.reportes;

import com.simreports.PostgresEmbebido;
import com.simreports.plantilla.PlantillaConfigRepository;
import com.simreports.plantilla.PlantillaService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de reportes contra la app completa (Postgres embebido real). */
@SpringBootTest
@AutoConfigureMockMvc
class ReporteControllerTest extends PostgresEmbebido {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void carpetaDeDatos(DynamicPropertyRegistry registro) {
        registro.add("simreports.almacenamiento.directorio", () -> datos.toString());
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private PlantillaService plantillas;
    @Autowired
    private ReporteGeneradoRepository reportes;
    @Autowired
    private PlantillaConfigRepository versiones;

    private final JsonMapper json = JsonMapper.builder().build();

    @AfterEach
    void limpiar() {
        reportes.deleteAll();
        versiones.deleteAll();
    }

    private static MockMultipartHttpServletRequestBuilder solicitud(String anio, String mes, byte[] telcel) {
        MockMultipartHttpServletRequestBuilder s = multipart("/api/reportes")
                .file(new MockMultipartFile("alestra", "Tráfico por línea.xlsx", null, ArchivosDePrueba.alestra()))
                .file(new MockMultipartFile("telcel", "Uso_de_datos.csv", "text/csv", telcel))
                .file(new MockMultipartFile("att", "att.csv", "text/csv", ArchivosDePrueba.att()));
        s.param("anio", anio).param("mes", mes);
        return s;
    }

    /** Genera un reporte y devuelve su id. */
    private long generar(String mes) throws Exception {
        String cuerpo = mvc.perform(solicitud("2026", mes, ArchivosDePrueba.telcel()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("id").asLong();
    }

    @Test
    void generarDevuelve201ConElDetalleYElReporteFinal() throws Exception {
        plantillas.registrarNuevaVersion(ArchivosDePrueba.plantilla());

        mvc.perform(solicitud("2026", "10", ArchivosDePrueba.telcel()))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/reportes/")))
                .andExpect(jsonPath("$.nombreArchivo").value("CONSUMOS_OCTUBRE_2026.xlsx"))
                .andExpect(jsonPath("$.periodoAnio").value(2026))
                .andExpect(jsonPath("$.periodoMes").value(10))
                .andExpect(jsonPath("$.versionPlantilla").value(1))
                .andExpect(jsonPath("$.urlDescarga", startsWith("/api/reportes/")))
                .andExpect(jsonPath("$.reporteFinal", startsWith("REPORTE FINAL DE LA CORRIDA")))
                .andExpect(jsonPath("$.archivosEntrada[*].fuente", contains("ALESTRA", "TELCEL", "ATT")))
                .andExpect(jsonPath("$.archivosEntrada[0].nombre").value("Tráfico por línea.xlsx"))
                .andExpect(jsonPath("$.consumoSinCliente[?(@.fuente == 'ALESTRA')].clave",
                        contains(ArchivosDePrueba.LINEA_SIN_CLIENTE)))
                .andExpect(jsonPath("$.avisos.totales", hasSize(2)))
                .andExpect(jsonPath("$.avisos.simsRepetidas", hasSize(0)));
    }

    @Test
    void listaElHistorialYMuestraElDetalleDeUnaCorrida() throws Exception {
        plantillas.registrarNuevaVersion(ArchivosDePrueba.plantilla());
        long septiembre = generar("9");
        long octubre = generar("10");

        mvc.perform(get("/api/reportes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains((int) octubre, (int) septiembre)))
                .andExpect(jsonPath("$[*].nombreArchivo",
                        contains("CONSUMOS_OCTUBRE_2026.xlsx", "CONSUMOS_SEPTIEMBRE_2026.xlsx")))
                .andExpect(jsonPath("$[0].reporteFinal").doesNotExist());
        mvc.perform(get("/api/reportes/" + septiembre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreArchivo").value("CONSUMOS_SEPTIEMBRE_2026.xlsx"))
                .andExpect(jsonPath("$.reporteFinal", containsString("Consumo sin cliente")));
    }

    @Test
    void descargaElXlsxConSuNombre() throws Exception {
        plantillas.registrarNuevaVersion(ArchivosDePrueba.plantilla());
        long id = generar("10");

        byte[] archivo = mvc.perform(get("/api/reportes/" + id + "/descargar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ReporteController.XLSX))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=\"CONSUMOS_OCTUBRE_2026.xlsx\"")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            assertThat(libro.getSheet("GEPP").getRow(1).getCell(3).getNumericCellValue()).isEqualTo(100.5);
            assertThat(libro.getSheet("BD_ALESTRA")).isNull();
        }
    }

    @Test
    void unReporteQueNoExisteDevuelve404() throws Exception {
        mvc.perform(get("/api/reportes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Reporte no encontrado"));
        mvc.perform(get("/api/reportes/999999/descargar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void erroresDeLaSolicitudDevuelven400ConProblemas() throws Exception {
        plantillas.registrarNuevaVersion(ArchivosDePrueba.plantilla());

        mvc.perform(solicitud("2026", "13", ArchivosDePrueba.telcel()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.problemas[0]", containsString("El mes debe estar entre 1 y 12")));
        mvc.perform(solicitud("2026", "10", ArchivosDePrueba.telcelTablaDinamica()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Archivo con formato inválido"))
                .andExpect(jsonPath("$.problemas[0]", startsWith("Archivo de Telcel ('Uso_de_datos.csv'): ")));
        mvc.perform(solicitud("2026", "10", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas[0]").value("El archivo de Telcel está vacío."));
        mvc.perform(multipart("/api/reportes").param("anio", "2026").param("mes", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problemas", hasSize(1)));

        assertThat(reportes.findAll()).isEmpty();
    }

    @Test
    void sinPlantillaActivaGenerarDevuelve409() throws Exception {
        mvc.perform(solicitud("2026", "10", ArchivosDePrueba.telcel()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.problemas[0]", containsString("Sube la plantilla")));
    }
}
