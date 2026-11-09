package com.simreports.reportes;

import com.simreports.PostgresEmbebido;
import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.plantilla.PlantillaConfigRepository;
import com.simreports.plantilla.PlantillaService;
import com.simreports.plantilla.SinPlantillaActivaException;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Corrida completa contra la app (Postgres embebido real), sin @Transactional de prueba. */
@SpringBootTest
class ReporteServiceTest extends PostgresEmbebido {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void carpetaDeDatos(DynamicPropertyRegistry registro) {
        registro.add("simreports.almacenamiento.directorio", () -> datos.toString());
    }

    @Autowired
    private ReporteService servicio;
    @Autowired
    private PlantillaService plantillas;
    @Autowired
    private ReporteGeneradoRepository reportes;
    @Autowired
    private PlantillaConfigRepository versiones;

    @AfterEach
    void limpiar() {
        reportes.deleteAll();
        versiones.deleteAll();
    }

    private static ArchivoEntrada alestra() {
        return new ArchivoEntrada("Tráfico por línea.xlsx", ArchivosDePrueba.alestra());
    }

    private static ArchivoEntrada telcel() {
        return new ArchivoEntrada("Uso_de_datos.csv", ArchivosDePrueba.telcel());
    }

    private static ArchivoEntrada att() {
        return new ArchivoEntrada("att.csv", ArchivosDePrueba.att());
    }

    @Test
    void generaElReporteLoGuardaYRegistraLaCorrida() throws IOException {
        plantillas.registrarNuevaVersion(ArchivosDePrueba.plantilla());

        ReporteResultado r = servicio.generar(new Periodo(2026, 10), alestra(), telcel(), att());

        ReporteGenerado registro = r.reporte();
        assertThat(registro.getId()).isNotNull();
        assertThat(registro.getNombreArchivo()).isEqualTo("CONSUMOS_OCTUBRE_2026.xlsx");
        assertThat(registro.getPeriodoAnio()).isEqualTo(2026);
        assertThat(registro.getPeriodoMes()).isEqualTo(10);
        assertThat(registro.getRutaArchivoResultado()).startsWith("reportes").endsWith("CONSUMOS_OCTUBRE_2026.xlsx");
        assertThat(registro.getArchivosEntrada()).contains("\"fuente\":\"ALESTRA\"", "Tráfico por línea.xlsx", "sha256");
        assertThat(registro.getLineasNoEncontradas()).contains("5599999999");
        assertThat(registro.getAvisos()).contains("\"totales\"", "\"simsRepetidas\"");
        assertThat(registro.getReporteFinal()).startsWith("REPORTE FINAL DE LA CORRIDA")
                .contains("ALESTRA: 1 líneas, 3.00 MB");
        assertThat(r.resultado().totalAsignado()).isEqualTo(107.75);

        byte[] archivo = Files.readAllBytes(datos.resolve(registro.getRutaArchivoResultado()));
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            assertThat(libro.getNumberOfSheets()).isEqualTo(1);
            XSSFSheet gepp = libro.getSheet("GEPP");
            assertThat(gepp.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(100.5);
            assertThat(gepp.getRow(2).getCell(3).getNumericCellValue()).isEqualTo(7.25);
        }
    }

    @Test
    void unArchivoDeOperadorInvalidoDiceCualEsYNoGuardaNada() throws IOException {
        plantillas.registrarNuevaVersion(ArchivosDePrueba.plantilla());
        long archivosAntes = contarReportesEnDisco();
        ArchivoEntrada telcelMalo = new ArchivoEntrada("tabla dinamica.csv", ArchivosDePrueba.telcelTablaDinamica());

        assertThatThrownBy(() -> servicio.generar(new Periodo(2026, 10), alestra(), telcelMalo, att()))
                .isInstanceOf(ArchivoFormatoInvalidoException.class)
                .hasMessageStartingWith("Archivo de Telcel ('tabla dinamica.csv'): ");

        assertThat(reportes.findAll()).isEmpty();
        assertThat(contarReportesEnDisco()).isEqualTo(archivosAntes);
    }

    private static long contarReportesEnDisco() throws IOException {
        Path carpeta = datos.resolve("reportes");
        if (!Files.exists(carpeta)) {
            return 0;
        }
        try (var archivos = Files.walk(carpeta)) {
            return archivos.filter(Files::isRegularFile).count();
        }
    }

    @Test
    void sinPlantillaActivaNoSeGeneraReporte() {
        assertThatThrownBy(() -> servicio.generar(new Periodo(2026, 10), alestra(), telcel(), att()))
                .isInstanceOf(SinPlantillaActivaException.class);
    }
}
