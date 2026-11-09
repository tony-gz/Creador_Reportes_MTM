package com.simreports.core.parsers;

import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

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

class TelcelParserTest {

    private final TelcelParser parser = new TelcelParser();

    @Test
    void parseaCsvRealConMetadataComillasYAgregacionPorFecha() {
        // Simula el archivo real: 7 líneas de metadata antes del
        // encabezado, ICCID/MSISDN entre comillas simples literales, el
        // mismo ICCID repetido en 2 fechas distintas (debe sumarse SIN
        // generar aviso de duplicado — es la forma normal del archivo),
        // y una fila con ICCID vacío (se excluye) — spec 02.
        String csv = String.join("\n",
                "Cuenta:,123456",
                "Usuario:,juan.perez",
                "Rango de fechas:,2026-08-01 a 2026-08-31",
                "Generado:,2026-09-01",
                "Reporte:,Consumo de datos",
                "Filtro:,Todos",
                "(metadata extra)",
                "DURACIÓN (UTC),ICCID,MSISDN,IMSI,USO (KB),USO (MB),USO (BYTES)",
                "2026-08-01,'8952020221396597611','5216641234567',444455556666,1024,1.0,",
                "2026-08-02,'8952020221396597611','5216641234567',444455556666,2048,2.0,",
                "2026-08-01,'8952020221396597622','5216647654321',444455556677,512,0.5,",
                "2026-08-03,'','5216640000000',444455559999,0,0,",
                ""
        );

        InputStream archivo = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        ResultadoParseo resultado = parser.parsear(archivo);

        assertEquals(2, resultado.consumos().size());
        assertEquals(3.0, resultado.consumos().get("8952020221396597611"));
        assertEquals(0.5, resultado.consumos().get("8952020221396597622"));

        assertTrue(resultado.avisos().stream().anyMatch(a -> a.tipo() == TipoAviso.CLAVE_VACIA_O_MALFORMADA));
        // Decisión de diseño explícita (ver ParserIccidConsumoGenerico):
        // nunca debe aparecer un aviso de duplicado en Telcel.
        assertFalse(resultado.avisos().stream().anyMatch(a -> a.tipo() == TipoAviso.CLAVE_DUPLICADA));
    }

    @Test
    void fallaSiNoEncuentraEncabezadoDentroDelLimite() {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            csv.append("linea de metadata sin encabezado real ").append(i).append("\n");
        }

        InputStream archivo = new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));

        assertThrows(ArchivoFormatoInvalidoException.class, () -> parser.parsear(archivo));
    }

    @Test
    void parseaVarianteXlsxConEncabezadoDesplazado() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet hoja = workbook.createSheet("Telcel");

            hoja.createRow(0).createCell(0).setCellValue("Reporte generado el 2026-09-01");

            Row header = hoja.createRow(1);
            header.createCell(0).setCellValue("ICCID");
            header.createCell(1).setCellValue("USO (MB)");

            Row fila1 = hoja.createRow(2);
            fila1.createCell(0).setCellValue("'8952000000000001'");
            fila1.createCell(1).setCellValue(10.5);

            Row fila2 = hoja.createRow(3);
            fila2.createCell(0).setCellValue("'8952000000000001'");
            fila2.createCell(1).setCellValue(4.5);

            workbook.write(out);
        }

        InputStream archivo = new ByteArrayInputStream(out.toByteArray());
        ResultadoParseo resultado = parser.parsear(archivo);

        assertEquals(1, resultado.consumos().size());
        assertEquals(15.0, resultado.consumos().get("8952000000000001"));
    }

    @Test
    void aceptaCsvEnWindows1252YConsumoConComaDecimal() {
        // Export guardado con la codificación por defecto de Excel en
        // Windows (no UTF-8): la "Ó" de DURACIÓN no es UTF-8 válido.
        String csv = String.join("\n",
                "DURACIÓN (UTC),ICCID,USO (MB)",
                "2026-08-01,'8952020221396597611',\"0,5\"",
                "2026-08-02,'8952020221396597611',\"1,25\"",
                "");

        InputStream archivo = new ByteArrayInputStream(csv.getBytes(java.nio.charset.Charset.forName("windows-1252")));
        ResultadoParseo resultado = parser.parsear(archivo);

        assertEquals(1.75, resultado.consumos().get("8952020221396597611"));
        assertTrue(resultado.avisos().isEmpty());
    }
}
