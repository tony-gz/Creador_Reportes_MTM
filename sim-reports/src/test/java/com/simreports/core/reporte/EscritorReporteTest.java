package com.simreports.core.reporte;

import com.simreports.core.matching.Asignacion;
import com.simreports.core.matching.FuenteConsumo;
import com.simreports.core.matching.ResultadoFilaPlantilla;
import com.simreports.core.matching.ResultadoTabla;
import com.simreports.core.plantilla.LectorPlantilla;
import com.simreports.core.plantilla.PlantillaLeida;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EscritorReporteTest {

    private byte[] plantilla;
    private PlantillaLeida leida;
    private short formatoConsumo;

    private static void tabla(XSSFSheet hoja, String nombre, int f0, int c0, int f1, int c1) {
        XSSFTable t = hoja.createTable(new AreaReference(new CellReference(f0, c0), new CellReference(f1, c1),
                SpreadsheetVersion.EXCEL2007));
        t.setName(nombre);
        t.setDisplayName(nombre);
        t.updateHeaders();
    }

    private static Row fila(XSSFSheet hoja, int i, String... textos) {
        Row r = hoja.createRow(i);
        for (int c = 0; c < textos.length; c++) {
            if (textos[c] != null) {
                r.createCell(c).setCellValue(textos[c]);
            }
        }
        return r;
    }

    /**
     * GEPP (cascada) con tres filas cuya celda de consumo trae, en orden, una
     * fórmula BUSCARV, un texto "NA" y un valor escrito a mano; una nota fuera
     * de la tabla. BIMBO (dual) con CONSUMO_TOTAL como fórmula. Y BD_ALESTRA.
     */
    @BeforeEach
    void armarPlantilla() throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle estilo = libro.createCellStyle();
            estilo.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));
            formatoConsumo = estilo.getDataFormat();

            XSSFSheet gepp = libro.createSheet("GEPP");
            fila(gepp, 0, "SIM", "LINEA", "ICCID", "CONSUMO(MB)");
            Row r1 = fila(gepp, 1, "ALESTRA", "5511110001", null);
            Cell f = r1.createCell(3);
            f.setCellFormula("IFERROR(VLOOKUP(B2,Z1:Z2,2,FALSE),0)");
            f.setCellStyle(estilo);
            Row r2 = fila(gepp, 2, "ALESTRA", "5511110002", null, "NA");
            CellStyle sinDecimales = libro.createCellStyle(); // como KUEHENE NAGEL: "#,##0" y en negritas
            sinDecimales.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
            org.apache.poi.ss.usermodel.Font negritas = libro.createFont();
            negritas.setBold(true);
            sinDecimales.setFont(negritas);
            r2.getCell(3).setCellStyle(sinDecimales);
            Row r3 = fila(gepp, 3, "TELCEL", null, "89520700000000000011");
            r3.createCell(3).setCellValue(14.65); // valor del RMS del mes pasado
            r3.getCell(3).setCellStyle(estilo);
            tabla(gepp, "Tabla3", 0, 0, 3, 3);
            gepp.createRow(6).createCell(0).setCellValue("STARLINK fuera de la tabla");
            gepp.getRow(6).createCell(3).setCellValue(777.0);

            XSSFSheet bimbo = libro.createSheet("BIMBO");
            fila(bimbo, 0, "LINEA_1", "LINEA_2", "CONSUMO(MB)_1", "CONSUMO(MB)_2", "CONSUMO_TOTAL");
            Row d = fila(bimbo, 1, "5511110003", "5511110004");
            d.createCell(2).setCellValue(1.0);
            d.createCell(3).setCellValue(1.0);
            d.createCell(4).setCellFormula("SUM(C2:D2)");
            tabla(bimbo, "Tabla1", 0, 0, 1, 4);

            XSSFSheet bd = libro.createSheet("BD_ALESTRA");
            fila(bd, 0, "LINEA", "Tráfico MB");
            libro.setActiveSheet(2); // la hoja activa es una BD_ que se va a quitar
            libro.write(out);
            plantilla = out.toByteArray();
        }
        LectorPlantilla lector = new LectorPlantilla();
        leida = lector.leer(new ByteArrayInputStream(plantilla), lector.detectar(new ByteArrayInputStream(plantilla)));
    }

    private static List<ResultadoTabla> resultados() {
        Asignacion cien = new Asignacion(100.5, FuenteConsumo.ALESTRA);
        return List.of(
                new ResultadoTabla("GEPP · Tabla3", List.of(
                        ResultadoFilaPlantilla.simple(cien),
                        ResultadoFilaPlantilla.simple(new Asignacion(7.25, FuenteConsumo.ALESTRA)),
                        ResultadoFilaPlantilla.simple(Asignacion.sinMatch()))),
                new ResultadoTabla("BIMBO · Tabla1", List.of(
                        ResultadoFilaPlantilla.dualSim(new Asignacion(10.0, FuenteConsumo.ALESTRA),
                                new Asignacion(20.0, FuenteConsumo.ALESTRA)))));
    }

    private XSSFWorkbook escribirYAbrir() throws IOException {
        ReporteEscrito escrito = new EscritorReporte().escribir(plantilla, leida, resultados());
        assertEquals(5, escrito.celdasEscritas());
        assertEquals(0, escrito.formulasSinRecalcular());
        return new XSSFWorkbook(new ByteArrayInputStream(escrito.contenido()));
    }

    @Test
    void escribeElConsumoComoNumeroEnLugarDeFormulasTextosYValoresAMano() throws IOException {
        try (XSSFWorkbook libro = escribirYAbrir()) {
            XSSFSheet gepp = libro.getSheet("GEPP");
            Cell formula = gepp.getRow(1).getCell(3);
            assertEquals(CellType.NUMERIC, formula.getCellType());
            assertEquals(100.5, formula.getNumericCellValue());
            assertEquals(7.25, gepp.getRow(2).getCell(3).getNumericCellValue()); // era "NA"
            assertEquals(0.0, gepp.getRow(3).getCell(3).getNumericCellValue()); // era 14.65 (D-015)
            for (int r = 1; r <= 3; r++) {
                assertEquals(formatoConsumo, gepp.getRow(r).getCell(3).getCellStyle().getDataFormat(),
                        "formato de dos decimales en la fila " + (r + 1));
            }
        }
    }

    @Test
    void todaCeldaDeConsumoQuedaConDosDecimalesSinPerderElRestoDelEstilo() throws IOException {
        try (XSSFWorkbook libro = escribirYAbrir()) {
            // "#,##0" en negritas → "#,##0.00" en negritas (D-022)
            CellStyle antesSinDecimales = libro.getSheet("GEPP").getRow(2).getCell(3).getCellStyle();
            assertEquals("#,##0.00", antesSinDecimales.getDataFormatString());
            assertTrue(libro.getFontAt(antesSinDecimales.getFontIndex()).getBold());
            // Celdas sin estilo (General) de la tabla dual
            Row d = libro.getSheet("BIMBO").getRow(1);
            assertEquals("#,##0.00", d.getCell(2).getCellStyle().getDataFormatString());
            assertEquals("#,##0.00", d.getCell(3).getCellStyle().getDataFormatString());
        }
    }

    @Test
    void noTocaLoQueEstaFueraDeLasTablas() throws IOException {
        try (XSSFWorkbook libro = escribirYAbrir()) {
            Row fuera = libro.getSheet("GEPP").getRow(6);
            assertEquals("STARLINK fuera de la tabla", fuera.getCell(0).getStringCellValue());
            assertEquals(777.0, fuera.getCell(3).getNumericCellValue());
        }
    }

    @Test
    void escribeLasDosSimsDeLaTablaDualYRecalculaElTotal() throws IOException {
        try (XSSFWorkbook libro = escribirYAbrir()) {
            Row d = libro.getSheet("BIMBO").getRow(1);
            assertEquals(10.0, d.getCell(2).getNumericCellValue());
            assertEquals(20.0, d.getCell(3).getNumericCellValue());
            assertEquals("SUM(C2:D2)", d.getCell(4).getCellFormula());
            assertEquals(30.0, d.getCell(4).getNumericCellValue());
            assertTrue(libro.getForceFormulaRecalculation());
        }
    }

    @Test
    void quitaLasHojasBdYDejaUnaHojaActivaValida() throws IOException {
        try (XSSFWorkbook libro = escribirYAbrir()) {
            List<String> hojas = new ArrayList<>();
            libro.forEach(h -> hojas.add(h.getSheetName()));
            assertEquals(List.of("GEPP", "BIMBO"), hojas);
            assertEquals(0, libro.getActiveSheetIndex());
        }
    }

    @Test
    void sumarConsumosReleeLoEscritoSinContarElTotal() {
        ReporteEscrito escrito = new EscritorReporte().escribir(plantilla, leida, resultados());

        assertEquals(100.5 + 7.25 + 0.0 + 10.0 + 20.0, EscritorReporte.sumarConsumos(escrito.contenido(), leida), 1e-9);
        // Sobre la plantilla sin escribir: solo cuentan las celdas numéricas (14.65 + 1 + 1).
        assertEquals(16.65, EscritorReporte.sumarConsumos(plantilla, leida), 1e-9);
    }

    @Test
    void rechazaResultadosQueNoCorrespondenALaPlantilla() {
        EscritorReporte escritor = new EscritorReporte();

        assertThrows(IllegalArgumentException.class, () -> escritor.escribir(plantilla, leida, resultados().subList(0, 1)));
        List<ResultadoTabla> filasDeMenos = List.of(
                new ResultadoTabla("GEPP · Tabla3", List.of(ResultadoFilaPlantilla.simple(Asignacion.sinMatch()))),
                resultados().get(1));
        assertThrows(IllegalArgumentException.class, () -> escritor.escribir(plantilla, leida, filasDeMenos));
    }
}
