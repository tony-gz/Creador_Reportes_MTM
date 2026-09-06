package com.simreports.core.parsers;

import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlestraParserTest {

    private final AlestraParser parser = new AlestraParser();

    @Test
    void parseaArchivoRealConMetadataPreviaYSumaDuplicados() throws IOException {
        // Simula el archivo real de Fase 0: 2 filas de metadata antes de
        // la tabla, encabezados reales "LINEA" / "Tráfico MB", una línea
        // duplicada (se suma) y una línea vacía (se excluye) — spec 02.
        String[][] datos = {
                {"6641111111", "100"},
                {"6642222222", "50"},
                {"6641111111", "25"},
                {"", "999"},
        };

        try (InputStream archivo = construirArchivoAlestra(datos)) {
            ResultadoParseo resultado = parser.parsear(archivo);

            assertEquals(2, resultado.consumos().size());
            assertEquals(125.0, resultado.consumos().get("6641111111"));
            assertEquals(50.0, resultado.consumos().get("6642222222"));

            assertTrue(resultado.avisos().stream().anyMatch(
                    a -> a.tipo() == TipoAviso.CLAVE_DUPLICADA && a.clave().equals("6641111111")));
            assertTrue(resultado.avisos().stream().anyMatch(
                    a -> a.tipo() == TipoAviso.CLAVE_VACIA_O_MALFORMADA));
        }
    }

    @Test
    void fallaSiElArchivoNoTieneNingunaTablaNativa() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Alestra");
            workbook.write(out);
        }

        assertThrows(ArchivoFormatoInvalidoException.class,
                () -> parser.parsear(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void fallaSiFaltaLaColumnaDeConsumo() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Alestra");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("LINEA");
            header.createCell(1).setCellValue("OTRA_COLUMNA");
            Row dato = sheet.createRow(1);
            dato.createCell(0).setCellValue("6641111111");
            dato.createCell(1).setCellValue("algo");

            crearTablaNativa(sheet, "Table1", 0, 0, 1, 1);
            workbook.write(out);
        }

        assertThrows(ArchivoFormatoInvalidoException.class,
                () -> parser.parsear(new ByteArrayInputStream(out.toByteArray())));
    }

    /**
     * Construye un .xlsx en memoria con 2 filas de metadata, una fila de
     * encabezado real ("LINEA", "Tráfico MB") en la fila 2, y los datos
     * dados a partir de la fila 3 — igual forma que el archivo real de
     * Alestra confirmado en Fase 0 (spec 02).
     */
    private InputStream construirArchivoAlestra(String[][] datos) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Alestra");

            sheet.createRow(0).createCell(0).setCellValue("Filtro: Agosto 2026");
            sheet.createRow(1).createCell(0).setCellValue("Generado: 2026-08-31");

            int filaEncabezado = 2;
            Row header = sheet.createRow(filaEncabezado);
            header.createCell(0).setCellValue("LINEA");
            header.createCell(1).setCellValue("Tráfico MB");

            int filaInicioDatos = filaEncabezado + 1;
            for (int i = 0; i < datos.length; i++) {
                Row fila = sheet.createRow(filaInicioDatos + i);
                fila.createCell(0).setCellValue(datos[i][0]);
                if (!datos[i][1].isEmpty()) {
                    fila.createCell(1).setCellValue(Double.parseDouble(datos[i][1]));
                }
            }

            int filaFinal = filaInicioDatos + datos.length - 1;
            crearTablaNativa(sheet, "Table1", filaEncabezado, 0, filaFinal, 1);

            workbook.write(out);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Crea una tabla nativa de Excel (Table) en el rango dado y fuerza
     * que los nombres de columna coincidan con el texto real de la fila
     * de encabezado, sin depender de que {@code createTable(...)} los
     * infiera automáticamente en toda versión de POI.
     */
    private void crearTablaNativa(XSSFSheet sheet, String nombre, int filaInicio, int colInicio,
                                   int filaFin, int colFin) {
        AreaReference area = new AreaReference(
                new CellReference(filaInicio, colInicio),
                new CellReference(filaFin, colFin),
                SpreadsheetVersion.EXCEL2007);
        XSSFTable tabla = sheet.createTable(area);
        tabla.setName(nombre);
        tabla.setDisplayName(nombre);

        Row headerRow = sheet.getRow(filaInicio);
        for (int i = 0; i < tabla.getColumns().size(); i++) {
            String nombreColumna = headerRow.getCell(colInicio + i).getStringCellValue();
            tabla.getColumns().get(i).setName(nombreColumna);
        }
    }
}
