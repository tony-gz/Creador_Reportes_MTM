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

    // ---------- Formato real del ATT consolidado (como la hoja BD_ATT) ----------

    @Test
    void leeVariosBloquesLadoALadoComoLaHojaBdAtt() throws IOException {
        // Forma real (octubre 2026): fila de títulos por cliente, encabezado
        // de Jasper "Uso de ciclo hasta la fecha (MB)", bloques de distinto
        // largo separados por una columna vacía, consumos como texto con
        // separador de miles y alguno como número.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet hoja = workbook.createSheet("BD_ATT");
            Row titulos = hoja.createRow(0);
            titulos.createCell(0).setCellValue("GEPP");
            titulos.createCell(3).setCellValue("Tiendas extra");
            titulos.createCell(6).setCellValue("LIVERPOOL");
            Row header = hoja.createRow(1);
            for (int c : new int[]{0, 3, 6}) {
                header.createCell(c).setCellValue("ICCID");
                header.createCell(c + 1).setCellValue("Uso de ciclo hasta la fecha (MB)");
            }
            Row r1 = hoja.createRow(2);
            r1.createCell(0).setCellValue("89520700000000000088");
            r1.createCell(1).setCellValue("0");
            r1.createCell(3).setCellValue("89520700000000000035");
            r1.createCell(4).setCellValue("1,170.94");
            r1.createCell(6).setCellValue("89520700000000000011");
            r1.createCell(7).setCellValue(27.82);
            Row r2 = hoja.createRow(3);
            r2.createCell(0).setCellValue("89520700000000000021");
            r2.createCell(1).setCellValue("5,191.615");
            // Bloques 2 y 3 ya terminaron: celdas vacías, sin aviso.
            Row r3 = hoja.createRow(4);
            r3.createCell(0).setCellValue("89520700000000000047");
            r3.createCell(1).setCellValue("27.596");

            workbook.write(out);
        }

        ResultadoParseo resultado = parser.parsear(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(5, resultado.consumos().size());
        assertEquals(0.0, resultado.consumos().get("89520700000000000088"));
        assertEquals(5191.615, resultado.consumos().get("89520700000000000021"));
        assertEquals(27.596, resultado.consumos().get("89520700000000000047"));
        assertEquals(1170.94, resultado.consumos().get("89520700000000000035"));
        assertEquals(27.82, resultado.consumos().get("89520700000000000011"));
        assertTrue(resultado.avisos().isEmpty(), () -> "avisos inesperados: " + resultado.avisos());
    }

    @Test
    void enVariosBloquesReportaElBloqueDeUnaFilaConProblema() {
        String csv = String.join("\n",
                "ICCID,Consumo_MB,,ICCID,Consumo_MB",
                "8952030000000001,1,,8952030000000002,abc",
                "");

        ResultadoParseo resultado = parser.parsear(csvUtf8(csv));

        assertEquals(1, resultado.consumos().size());
        assertTrue(resultado.avisos().stream().anyMatch(a -> a.detalle().contains("bloque 2")));
    }

    @Test
    void fallaSiUnBloqueNoTieneColumnaDeConsumo() {
        String csv = String.join("\n",
                "ICCID,ICCID,Consumo_MB",
                "8952030000000001,8952030000000002,1",
                "");

        assertThrows(ArchivoFormatoInvalidoException.class, () -> parser.parsear(csvUtf8(csv)));
    }

    @Test
    void ignoraSinAvisoLasFilasCompletamenteVacias() {
        String csv = "ICCID,Consumo_MB\n8952030000000001,1\n,\n  ,  \n8952030000000002,2\n";

        ResultadoParseo resultado = parser.parsear(csvUtf8(csv));

        assertEquals(2, resultado.consumos().size());
        assertTrue(resultado.avisos().isEmpty());
    }

    private static InputStream csvUtf8(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
