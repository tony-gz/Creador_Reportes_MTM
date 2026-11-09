package com.simreports.reportes;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Plantilla y archivos de operadores mínimos (con datos inventados) para las
 * pruebas de reportes: GEPP con una SIM de Alestra (100.5 MB) y una de Telcel
 * (7.25 MB), más una línea de Alestra sin cliente (3 MB).
 */
final class ArchivosDePrueba {

    static final String LINEA = "5511110001";
    static final String ICCID = "89520700000000000011";
    static final String LINEA_SIN_CLIENTE = "5599999999";

    private ArchivosDePrueba() {
    }

    private static void tabla(XSSFSheet hoja, String nombre, int f0, int c0, int f1, int c1) {
        XSSFTable t = hoja.createTable(new AreaReference(new CellReference(f0, c0), new CellReference(f1, c1),
                SpreadsheetVersion.EXCEL2007));
        t.setName(nombre);
        t.setDisplayName(nombre);
        t.updateHeaders();
    }

    private static byte[] bytes(XSSFWorkbook libro) {
        try (libro; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            libro.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** GEPP con una SIM de Alestra y una de Telcel; hoja BD_ALESTRA con datos viejos. */
    static byte[] plantilla() {
        XSSFWorkbook libro = new XSSFWorkbook();
        XSSFSheet gepp = libro.createSheet("GEPP");
        String[] enc = {"SIM", "LINEA", "ICCID", "CONSUMO(MB)"};
        Row h = gepp.createRow(0);
        for (int i = 0; i < enc.length; i++) {
            h.createCell(i).setCellValue(enc[i]);
        }
        Row a = gepp.createRow(1);
        a.createCell(0).setCellValue("ALESTRA");
        a.createCell(1).setCellValue(LINEA);
        Row t = gepp.createRow(2);
        t.createCell(0).setCellValue("TELCEL");
        t.createCell(2).setCellValue(ICCID);
        tabla(gepp, "Tabla3", 0, 0, 2, 3);
        libro.createSheet("BD_ALESTRA").createRow(0).createCell(0).setCellValue("datos viejos");
        return bytes(libro);
    }

    static byte[] alestra() {
        XSSFWorkbook libro = new XSSFWorkbook();
        XSSFSheet hoja = libro.createSheet("Tráfico");
        Row h = hoja.createRow(0);
        h.createCell(0).setCellValue("LINEA");
        h.createCell(1).setCellValue("Tráfico MB");
        Row d1 = hoja.createRow(1);
        d1.createCell(0).setCellValue(LINEA);
        d1.createCell(1).setCellValue(100.5);
        Row d2 = hoja.createRow(2);
        d2.createCell(0).setCellValue(LINEA_SIN_CLIENTE);
        d2.createCell(1).setCellValue(3.0);
        tabla(hoja, "Table1", 0, 0, 2, 1);
        return bytes(libro);
    }

    static byte[] telcel() {
        return ("ICCID,USO (MB)\n" + ICCID + ",7.25\n").getBytes(StandardCharsets.UTF_8);
    }

    /** La tabla dinámica de Telcel, que el parser rechaza. */
    static byte[] telcelTablaDinamica() {
        return "Etiquetas de fila,Suma de USO (MB)\nx,1\n".getBytes(StandardCharsets.UTF_8);
    }

    static byte[] att() {
        return "ICCID,Consumo_MB\n89520700000000000099,0\n".getBytes(StandardCharsets.UTF_8);
    }
}
