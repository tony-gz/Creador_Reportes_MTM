package com.simreports.core.parsers;

import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttParserTest {

    private final AttParser parser = new AttParser();

    @Test
    void parseaArchivoYaConsolidadoDeDosColumnas() {
        // Spec 02: archivo ya consolidado por el usuario, sin filas de
        // metadata, exactamente ICCID + Consumo_MB.
        String csv = String.join("\n",
                "ICCID,Consumo_MB",
                "8952030000000001,10.5",
                "8952030000000002,5.25",
                ""
        );

        InputStream archivo = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        ResultadoParseo resultado = parser.parsear(archivo);

        assertEquals(2, resultado.consumos().size());
        assertEquals(10.5, resultado.consumos().get("8952030000000001"));
        assertEquals(5.25, resultado.consumos().get("8952030000000002"));
        assertTrue(resultado.avisos().isEmpty());
    }

    @Test
    void excluyeFilaConIccidVacioYLoReportaComoAviso() {
        String csv = String.join("\n",
                "ICCID,Consumo_MB",
                "8952030000000001,10.5",
                ",99.0",
                ""
        );

        InputStream archivo = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        ResultadoParseo resultado = parser.parsear(archivo);

        assertEquals(1, resultado.consumos().size());
        assertTrue(resultado.avisos().stream().anyMatch(a -> a.tipo() == TipoAviso.CLAVE_VACIA_O_MALFORMADA));
    }

    @Test
    void fallaSiNoTieneLasColumnasObligatorias() {
        String csv = String.join("\n",
                "ICCID,OTRA_COLUMNA",
                "8952030000000001,algo",
                ""
        );

        InputStream archivo = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(ArchivoFormatoInvalidoException.class, () -> parser.parsear(archivo));
    }

    // ---------- CSV: casos reales de exports y de "Guardar como" en Excel ----------

    @Test
    void aceptaCsvGuardadoComoUtf8ConBom() {
        // Regresión: el BOM queda pegado a "ICCID" (primera columna) y
        // antes impedía reconocer el encabezado.
        String csv = "\uFEFFICCID,Consumo_MB\n8952030000000001,10.5\n";

        ResultadoParseo resultado = parser.parsear(csvUtf8(csv));

        assertEquals(10.5, resultado.consumos().get("8952030000000001"));
    }

    @Test
    void aceptaCamposEntreComillasDoblesConComasDentro() {
        // Regresión: encabezados y valores entre comillas dobles, y un
        // consumo con separador de miles que antes partía la fila en dos.
        String csv = String.join("\n",
                "\"ICCID\",\"Consumo_MB\"",
                "\"8952030000000001\",\"1,234.5\"",
                "\"8952030000000002\",\"7\"",
                "");

        ResultadoParseo resultado = parser.parsear(csvUtf8(csv));

        assertEquals(2, resultado.consumos().size());
        assertEquals(1234.5, resultado.consumos().get("8952030000000001"));
        assertEquals(7.0, resultado.consumos().get("8952030000000002"));
        assertTrue(resultado.avisos().isEmpty());
    }

    @Test
    void aceptaFinesDeLineaWindowsYComillaDobleEscapada() {
        String csv = "ICCID,Consumo_MB,Nota\r\n"
                + "8952030000000001,2.5,\"dice \"\"hola\"\", con coma\"\r\n"
                + "8952030000000002,1.5,\"nota en\r\ndos lineas\"\r\n";

        ResultadoParseo resultado = parser.parsear(csvUtf8(csv));

        assertEquals(2, resultado.consumos().size());
        assertEquals(2.5, resultado.consumos().get("8952030000000001"));
        assertEquals(1.5, resultado.consumos().get("8952030000000002"));
        assertTrue(resultado.avisos().isEmpty());
    }

    @Test
    void quitaEspaciosInvisiblesAlrededorDelIccid() {
        String csv = "ICCID,Consumo_MB\n\u00A08952030000000001\u00A0,3\n";

        ResultadoParseo resultado = parser.parsear(csvUtf8(csv));

        assertEquals(3.0, resultado.consumos().get("8952030000000001"));
    }

    @Test
    void fallaDeFormaExplicitaSiUnaComillaDobleNuncaSeCierra() {
        String csv = "ICCID,Consumo_MB\n\"8952030000000001,3\n8952030000000002,4\n";

        assertThrows(ArchivoFormatoInvalidoException.class, () -> parser.parsear(csvUtf8(csv)));
    }

    // ---------- XLSX: valores numéricos reales, no el texto mostrado ----------

    @Test
    void xlsxLeeElConsumoRealSinElRedondeoDelFormatoDeCelda() throws IOException {
        // Regresión: con formato "#,##0.00", DataFormatter devolvía el
        // valor mostrado (1,234.57 y 0.00) en vez del real.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet hoja = workbook.createSheet("ATT");
            CellStyle dosDecimales = workbook.createCellStyle();
            dosDecimales.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            Row header = hoja.createRow(0);
            header.createCell(0).setCellValue("ICCID");
            header.createCell(1).setCellValue("Consumo_MB");

            Row fila1 = hoja.createRow(1);
            fila1.createCell(0).setCellValue("8952030000000001");
            Cell c1 = fila1.createCell(1);
            c1.setCellValue(1234.5678);
            c1.setCellStyle(dosDecimales);

            Row fila2 = hoja.createRow(2);
            fila2.createCell(0).setCellValue("8952030000000002");
            Cell c2 = fila2.createCell(1);
            c2.setCellValue(0.004);
            c2.setCellStyle(dosDecimales);

            workbook.write(out);
        }

        ResultadoParseo resultado = parser.parsear(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(1234.5678, resultado.consumos().get("8952030000000001"));
        assertEquals(0.004, resultado.consumos().get("8952030000000002"));
    }

    @Test
    void xlsxUsaElResultadoCacheadoDeUnaFormulaDeConsumo() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet hoja = workbook.createSheet("ATT");
            Row header = hoja.createRow(0);
            header.createCell(0).setCellValue("ICCID");
            header.createCell(1).setCellValue("Consumo_MB");
            header.createCell(2).setCellValue("A");
            header.createCell(3).setCellValue("B");

            Row fila = hoja.createRow(1);
            fila.createCell(0).setCellValue("8952030000000001");
            fila.createCell(2).setCellValue(1.25);
            fila.createCell(3).setCellValue(2.5);
            fila.createCell(1).setCellFormula("C2+D2");
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            workbook.write(out);
        }

        ResultadoParseo resultado = parser.parsear(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(3.75, resultado.consumos().get("8952030000000001"));
    }

    @Test
    void xlsxExcluyeConAvisoUnIccidGuardadoComoNumero() throws IOException {
        // Un ICCID de 19 dígitos guardado como número ya perdió dígitos en
        // el propio archivo: no debe cargarse como "8.95202E+18".
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet hoja = workbook.createSheet("ATT");
            Row header = hoja.createRow(0);
            header.createCell(0).setCellValue("ICCID");
            header.createCell(1).setCellValue("Consumo_MB");

            Row numerico = hoja.createRow(1);
            numerico.createCell(0).setCellValue(8952020221396597611d);
            numerico.createCell(1).setCellValue(12.0);

            Row texto = hoja.createRow(2);
            texto.createCell(0).setCellValue("8952020221396597622");
            texto.createCell(1).setCellValue(5.0);

            workbook.write(out);
        }

        ResultadoParseo resultado = parser.parsear(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(1, resultado.consumos().size());
        assertEquals(5.0, resultado.consumos().get("8952020221396597622"));
        assertFalse(resultado.consumos().keySet().stream().anyMatch(k -> k.contains("E")));
        assertTrue(resultado.avisos().stream().anyMatch(a ->
                a.tipo() == TipoAviso.CLAVE_VACIA_O_MALFORMADA
                        && a.detalle().contains("guardado como número")
                        && a.detalle().contains("12.0 MB")));
    }

    private static InputStream csvUtf8(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
