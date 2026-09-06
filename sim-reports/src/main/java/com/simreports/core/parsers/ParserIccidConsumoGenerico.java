package com.simreports.core.parsers;

import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.AvisoParseo;
import com.simreports.core.HeaderResolver;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Motor interno compartido por los parsers de Telcel y ATT — ver spec
 * 02, sección "ATT" ("El parser es idéntico al de Telcel
 * post-agregación"). No implementa {@code OrigenParser} directamente;
 * cada operador expone su propia clase pública (p. ej. {@link
 * TelcelParser}) que delega aquí con su propio {@link HeaderResolver}.
 *
 * <p>Soporta tanto {@code .csv} como {@code .xlsx} sin depender del
 * nombre de archivo (spec 02: "nombre de archivo arbitrario"): detecta
 * el formato por el contenido (firma ZIP {@code PK} al inicio = xlsx,
 * si no, se trata como texto CSV).</p>
 *
 * <p>Ninguno de los dos formatos asume que el encabezado real está en la
 * primera fila: busca la primera fila, dentro de un límite razonable de
 * filas, que resuelva las dos columnas obligatorias (ICCID y consumo) —
 * ver spec 02, sección "Filas de metadata antes del encabezado real".
 * </p>
 */
class ParserIccidConsumoGenerico {

    private static final String CAMPO_ICCID = "ICCID";
    private static final String CAMPO_CONSUMO = "CONSUMO";
    private static final int LIMITE_FILAS_BUSQUEDA_ENCABEZADO = 20;

    private final HeaderResolver headerResolver;
    private final String nombreOperador;

    ParserIccidConsumoGenerico(HeaderResolver headerResolver, String nombreOperador) {
        this.headerResolver = Objects.requireNonNull(headerResolver, "headerResolver no puede ser null");
        this.nombreOperador = Objects.requireNonNull(nombreOperador, "nombreOperador no puede ser null");
    }

    ResultadoParseo parsear(InputStream archivo) {
        Objects.requireNonNull(archivo, "archivo no puede ser null");

        byte[] contenido;
        try {
            contenido = archivo.readAllBytes();
        } catch (IOException e) {
            throw new ArchivoFormatoInvalidoException(
                    "No se pudo leer el archivo de " + nombreOperador + ": " + e.getMessage(), e);
        }

        List<List<String>> filas = pareceXlsx(contenido) ? extraerFilasXlsx(contenido) : extraerFilasCsv(contenido);
        Encabezado encabezado = buscarEncabezado(filas);

        Map<String, Double> acumulado = new LinkedHashMap<>();
        List<AvisoParseo> avisos = new ArrayList<>();

        for (int r = encabezado.filaIndice() + 1; r < filas.size(); r++) {
            List<String> fila = filas.get(r);

            String iccid = normalizarValorTexto(obtenerCelda(fila, encabezado.colIccid()));
            if (iccid.isEmpty()) {
                avisos.add(new AvisoParseo(
                        TipoAviso.CLAVE_VACIA_O_MALFORMADA,
                        "",
                        "Fila " + (r + 1) + " de " + nombreOperador + ": ICCID vacío o no legible, se "
                                + "excluyó de la suma."));
                continue;
            }

            Double consumo = parsearNumero(obtenerCelda(fila, encabezado.colConsumo()));
            if (consumo == null) {
                avisos.add(new AvisoParseo(
                        TipoAviso.CLAVE_VACIA_O_MALFORMADA,
                        iccid,
                        "Fila " + (r + 1) + " de " + nombreOperador + ": el consumo no es un valor numérico "
                                + "legible, se excluyó de la suma."));
                continue;
            }

            // Nota de diseño: a diferencia de Alestra, aquí NO se genera
            // un aviso de "clave duplicada" cuando el mismo ICCID
            // aparece varias veces — es la forma normal del archivo
            // (una fila por fecha, spec 02), no una anomalía a reportar.
            acumulado.merge(iccid, consumo, Double::sum);
        }

        return new ResultadoParseo(acumulado, avisos);
    }

    private record Encabezado(int filaIndice, int colIccid, int colConsumo) {
    }

    private Encabezado buscarEncabezado(List<List<String>> filas) {
        int limite = Math.min(LIMITE_FILAS_BUSQUEDA_ENCABEZADO, filas.size());
        for (int r = 0; r < limite; r++) {
            List<String> fila = filas.get(r);
            Integer colIccid = null;
            Integer colConsumo = null;
            for (int c = 0; c < fila.size(); c++) {
                Optional<String> campo = headerResolver.resolver(fila.get(c));
                if (campo.isPresent() && campo.get().equals(CAMPO_ICCID) && colIccid == null) {
                    colIccid = c;
                }
                if (campo.isPresent() && campo.get().equals(CAMPO_CONSUMO) && colConsumo == null) {
                    colConsumo = c;
                }
            }
            if (colIccid != null && colConsumo != null) {
                return new Encabezado(r, colIccid, colConsumo);
            }
        }
        throw new ArchivoFormatoInvalidoException(
                "No se encontró la fila de encabezado con las columnas obligatorias ICCID y de consumo en "
                        + "el archivo de " + nombreOperador + " dentro de las primeras " + limite + " filas.");
    }

    private boolean pareceXlsx(byte[] contenido) {
        return contenido.length >= 2 && contenido[0] == 0x50 && contenido[1] == 0x4B; // firma ZIP "PK"
    }

    private List<List<String>> extraerFilasXlsx(byte[] contenido) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            XSSFSheet hoja = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<List<String>> filas = new ArrayList<>();
            int ultimaFila = hoja.getLastRowNum();
            for (int r = 0; r <= ultimaFila; r++) {
                Row fila = hoja.getRow(r);
                List<String> celdas = new ArrayList<>();
                if (fila != null) {
                    int ultimaCelda = Math.max(fila.getLastCellNum(), 0);
                    for (int c = 0; c < ultimaCelda; c++) {
                        Cell celda = fila.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        celdas.add(formatter.formatCellValue(celda));
                    }
                }
                filas.add(celdas);
            }
            return filas;
        } catch (IOException e) {
            throw new ArchivoFormatoInvalidoException(
                    "No se pudo leer el archivo de " + nombreOperador + " como .xlsx: " + e.getMessage(), e);
        }
    }

    private List<List<String>> extraerFilasCsv(byte[] contenido) {
        List<List<String>> filas = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(contenido), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                filas.add(Arrays.asList(linea.split(",", -1)));
            }
        } catch (IOException e) {
            throw new ArchivoFormatoInvalidoException(
                    "No se pudo leer el archivo de " + nombreOperador + " como .csv: " + e.getMessage(), e);
        }
        return filas;
    }

    private String obtenerCelda(List<String> fila, int indice) {
        if (indice < 0 || indice >= fila.size()) {
            return "";
        }
        String valor = fila.get(indice);
        return valor == null ? "" : valor;
    }

    /**
     * Normaliza un valor de celda a texto: quita espacios al inicio/fin
     * y, si el valor viene envuelto en comillas simples literales (caso
     * real de Telcel para ICCID/MSISDN, spec 02), las remueve.
     */
    private String normalizarValorTexto(String valor) {
        String v = valor.trim();
        if (v.length() >= 2 && v.startsWith("'") && v.endsWith("'")) {
            v = v.substring(1, v.length() - 1);
        }
        return v.trim();
    }

    private Double parsearNumero(String texto) {
        String limpio = normalizarValorTexto(texto).replace(",", "");
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
