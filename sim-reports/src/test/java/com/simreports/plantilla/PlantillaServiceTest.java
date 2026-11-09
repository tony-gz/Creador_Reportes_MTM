package com.simreports.plantilla;

import com.simreports.PostgresEmbebido;
import com.simreports.core.matching.FilaPlantilla;
import com.simreports.core.plantilla.MapeoPlantilla;
import com.simreports.core.plantilla.PlantillaInvalidaException;
import com.simreports.core.plantilla.TipoTablaPlantilla;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sin {@code @Transactional} de prueba a propósito: el servicio debe hacer
 * commit de verdad para que la restricción diferida "una sola activa" se
 * compruebe. Los datos se limpian después de cada test.
 */
@SpringBootTest
class PlantillaServiceTest extends PostgresEmbebido {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void carpetaDeDatos(DynamicPropertyRegistry registro) {
        registro.add("simreports.almacenamiento.directorio", () -> datos.toString());
    }

    @Autowired
    private PlantillaService servicio;

    @Autowired
    private PlantillaConfigRepository repositorio;

    @AfterEach
    void limpiar() throws IOException {
        repositorio.deleteAll();
        try (Stream<Path> archivos = Files.walk(datos)) {
            archivos.sorted((a, b) -> b.compareTo(a)).filter(p -> !p.equals(datos)).forEach(p -> p.toFile().delete());
        }
    }

    /** Plantilla mínima válida: hoja GEPP con una tabla cascada y una línea. */
    private static byte[] plantilla(String linea) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet hoja = libro.createSheet("GEPP");
            Row encabezado = hoja.createRow(5);
            String[] nombres = {"SIM", "LINEA", "ICCID", "CONSUMO(MB)"};
            for (int i = 0; i < nombres.length; i++) {
                encabezado.createCell(i).setCellValue(nombres[i]);
            }
            Row datos = hoja.createRow(6);
            datos.createCell(0).setCellValue("ALESTRA");
            datos.createCell(1).setCellValue(linea);
            datos.createCell(3).setCellValue(0.0);
            XSSFTable tabla = hoja.createTable(new AreaReference(new CellReference(5, 0), new CellReference(6, 3),
                    SpreadsheetVersion.EXCEL2007));
            tabla.setName("Tabla3");
            tabla.setDisplayName("Tabla3");
            tabla.updateHeaders();
            libro.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void laPrimeraSubidaCreaLaVersion1ActivaYGuardaElArchivoYElMapeo() throws IOException {
        byte[] archivo = plantilla("5511112222");

        PlantillaConfig v1 = servicio.registrarNuevaVersion(archivo).version();

        assertThat(v1.getVersion()).isEqualTo(1);
        assertThat(v1.isActiva()).isTrue();
        assertThat(Files.readAllBytes(datos.resolve(v1.getRutaArchivo()))).isEqualTo(archivo);
        MapeoPlantilla mapeo = servicio.activa().orElseThrow().mapeo();
        assertThat(mapeo.tablas()).hasSize(1);
        assertThat(mapeo.tablas().get(0).tipo()).isEqualTo(TipoTablaPlantilla.SIMPLE_CASCADA);
        assertThat(mapeo.tablas().get(0).identificador()).isEqualTo("GEPP · Tabla3");
    }

    @Test
    void unaSubidaNuevaDesactivaLaAnteriorYConservaSuArchivo() throws IOException {
        PlantillaConfig v1 = servicio.registrarNuevaVersion(plantilla("5511112222")).version();

        PlantillaConfig v2 = servicio.registrarNuevaVersion(plantilla("5511113333")).version();

        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(repositorio.findByActivaTrue()).get().extracting(PlantillaConfig::getId).isEqualTo(v2.getId());
        assertThat(repositorio.findByVersion(1)).get().extracting(PlantillaConfig::isActiva).isEqualTo(false);
        assertThat(Files.exists(datos.resolve(v1.getRutaArchivo()))).isTrue();
        assertThat(v2.getRutaArchivo()).isNotEqualTo(v1.getRutaArchivo());
    }

    @Test
    void unaPlantillaConProblemasNoGuardaNadaYConservaLaActiva() throws IOException {
        PlantillaConfig v1 = servicio.registrarNuevaVersion(plantilla("5511112222")).version();
        byte[] sinTablas;
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Row fila = libro.createSheet("DENTALIA").createRow(5);
            fila.createCell(0).setCellValue("LINEA");
            fila.createCell(1).setCellValue("CONSUMO(MB)");
            libro.write(out);
            sinTablas = out.toByteArray();
        }

        assertThatThrownBy(() -> servicio.registrarNuevaVersion(sinTablas))
                .isInstanceOf(PlantillaInvalidaException.class)
                .hasMessageContaining("DENTALIA");

        assertThat(repositorio.findAll()).hasSize(1);
        assertThat(repositorio.findByActivaTrue()).get().extracting(PlantillaConfig::getId).isEqualTo(v1.getId());
        assertThat(Files.exists(datos.resolve("plantillas/v2"))).isFalse();
    }

    @Test
    void leeLaPlantillaActivaParaUnReporteConElMapeoGuardado() throws IOException {
        byte[] archivo = plantilla("5511112222");
        PlantillaConfig v1 = servicio.registrarNuevaVersion(archivo).version();

        PlantillaParaReporte paraReporte = servicio.leerActivaParaReporte();

        assertThat(paraReporte.version().getId()).isEqualTo(v1.getId());
        assertThat(paraReporte.contenido()).isEqualTo(archivo);
        assertThat(paraReporte.leida().tablasConFilas()).hasSize(1);
        assertThat(paraReporte.leida().tablasConFilas().get(0).filas())
                .containsExactly(new FilaPlantilla("5511112222", null, null, null, "ALESTRA", 7));
    }

    @Test
    void sinPlantillaActivaNoSePuedeGenerarUnReporte() {
        assertThat(servicio.activa()).isEmpty();
        assertThatThrownBy(() -> servicio.leerActivaParaReporte())
                .isInstanceOf(SinPlantillaActivaException.class);
    }
}
