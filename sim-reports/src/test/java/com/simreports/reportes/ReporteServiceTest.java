package com.simreports.reportes;

import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.plantilla.PlantillaInvalidaException;
import com.simreports.plantilla.PlantillaService;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Servicio de reportes sin estado (D-023): no necesita base de datos ni disco. */
class ReporteServiceTest {

    private final ReporteService servicio = new ReporteService(new PlantillaService());

    private static ArchivoEntrada plantilla() {
        return new ArchivoEntrada("CONSUMOS_SEPTIEMBRE_2026.xlsx", ArchivosDePrueba.plantilla());
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
    void generaElReporteEnMemoriaConSuReporteFinal() throws IOException {
        ReporteResultado r = servicio.generar(new Periodo(2026, 10), plantilla(), alestra(), telcel(), att());

        assertThat(r.nombreArchivo()).isEqualTo("CONSUMOS_OCTUBRE_2026.xlsx");
        assertThat(r.reporteFinal()).startsWith("REPORTE FINAL DE LA CORRIDA").contains("ALESTRA: 1 líneas, 3.00 MB");
        assertThat(r.resultado().totalAsignado()).isEqualTo(107.75);
        assertThat(r.archivosEntrada()).extracting(ReporteResultado.ArchivoUsado::fuente)
                .containsExactly("PLANTILLA", "ALESTRA", "TELCEL", "ATT");
        assertThat(r.archivosEntrada().get(1).nombre()).isEqualTo("Tráfico por línea.xlsx");

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(r.contenido()))) {
            assertThat(libro.getNumberOfSheets()).isEqualTo(1);
            XSSFSheet gepp = libro.getSheet("GEPP");
            assertThat(gepp.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(100.5);
            assertThat(gepp.getRow(2).getCell(3).getNumericCellValue()).isEqualTo(7.25);
        }
    }

    @Test
    void unArchivoDeOperadorInvalidoDiceCualEs() {
        ArchivoEntrada telcelMalo = new ArchivoEntrada("tabla dinamica.csv", ArchivosDePrueba.telcelTablaDinamica());

        assertThatThrownBy(() -> servicio.generar(new Periodo(2026, 10), plantilla(), alestra(), telcelMalo, att()))
                .isInstanceOf(ArchivoFormatoInvalidoException.class)
                .hasMessageStartingWith("Archivo de Telcel ('tabla dinamica.csv'): ");
    }

    @Test
    void unaPlantillaInvalidaNoGeneraReporte() {
        ArchivoEntrada vacia = new ArchivoEntrada("vacia.xlsx", new byte[0]);

        assertThatThrownBy(() -> servicio.generar(new Periodo(2026, 10), vacia, alestra(), telcel(), att()))
                .isInstanceOf(PlantillaInvalidaException.class);
    }
}
