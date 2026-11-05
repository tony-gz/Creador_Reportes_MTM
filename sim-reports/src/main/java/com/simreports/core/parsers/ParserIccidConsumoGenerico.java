package com.simreports.core.parsers;

import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.AvisoParseo;
import com.simreports.core.HeaderResolver;
import com.simreports.core.NormalizadorClave;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
 *
 * <p><b>Lectura de .xlsx.</b> Cada celda conserva su texto <i>y</i>, si
 * es numérica, su valor numérico real ({@link Celda}). Así:</p>
 * <ul>
 *   <li>El consumo se toma del valor numérico real, no del texto mostrado
 *       por {@code DataFormatter} (que aplica el formato de celda: con
 *       {@code #,##0.00}, {@code 1234.5678} se mostraría como
 *       {@code 1,234.57} y {@code 0.004} como {@code 0.00}).</li>
 *   <li>Un ICCID guardado como número en Excel ya viene dañado en el
 *       propio archivo (un {@code double} no conserva los 19-20 dígitos
 *       de un ICCID) y se mostraría como {@code 8.95202E+18}. Esa fila se
 *       excluye con un aviso explícito en vez de cargar una clave
 *       corrupta que nunca va a coincidir.</li>
 * </ul>
 *
 * <p><b>Lectura de .csv.</b> Lector RFC 4180: respeta campos entre
 * comillas dobles (con comas o saltos de línea dentro, y {@code ""} como
 * comilla escapada), y descarta el BOM UTF-8 inicial si el archivo se
 * guardó como "UTF-8 con BOM" (caso típico al guardar el ATT consolidado
 * desde Excel). Si el contenido no es UTF-8 válido, se lee como
 * Windows-1252 (codificación por defecto de Excel en Windows en español)
 * en vez de llenar el texto de caracteres de reemplazo.</p>
 *
 * <p>Las claves (ICCID) se limpian con {@link NormalizadorClave} y los
 * consumos escritos como texto se leen con {@link LectorNumero}, igual que
 * en el resto de parsers.</p>
 */
class ParserIccidConsumoGenerico {

    private static final String CAMPO_ICCID = "ICCID";
    private static final String CAMPO_CONSUMO = "CONSUMO";
    private static final int LIMITE_FILAS_BUSQUEDA_ENCABEZADO = 20;

    /**
     * Mayor entero que un {@code double} (y Excel, que guarda 15 dígitos
     * significativos) representa sin redondeo. Un ICCID numérico por
     * encima de este valor ya perdió dígitos.
     */
    private static final double MAX_ENTERO_EXACTO = 1e15;

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

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

        List<List<Celda>> filas = pareceXlsx(contenido) ? extraerFilasXlsx(contenido) : extraerFilasCsv(contenido);
        Encabezado encabezado = buscarEncabezado(filas);

        Map<String, Double> acumulado = new LinkedHashMap<>();
        List<AvisoParseo> avisos = new ArrayList<>();

        for (int r = encabezado.filaIndice() + 1; r < filas.size(); r++) {
            List<Celda> fila = filas.get(r);
            Celda celdaIccid = obtenerCelda(fila, encabezado.colIccid());
            Celda celdaConsumo = obtenerCelda(fila, encabezado.colConsumo());

            if (celdaIccid.esNumerica() && !esEnteroExacto(celdaIccid.numero())) {
                Double consumoPerdido = leerConsumo(celdaConsumo);
                avisos.add(new AvisoParseo(
                        TipoAviso.CLAVE_VACIA_O_MALFORMADA,
                        celdaIccid.texto(),
                        "Fila " + (r + 1) + " de " + nombreOperador + ": el ICCID está guardado como número "
                                + "en Excel (se muestra como '" + celdaIccid.texto() + "'). Un número no "
                                + "conserva todos los dígitos de un ICCID, así que el valor ya viene dañado "
                                + "en el archivo; se excluyó de la suma"
                                + (consumoPerdido != null ? " (" + consumoPerdido + " MB)" : "")
                                + ". Formatea la columna ICCID como Texto en el archivo de origen y vuelve a "
                                + "pegar los valores."));
                continue;
            }

            String iccid = celdaIccid.esNumerica()
                    ? String.valueOf((long) celdaIccid.numero().doubleValue())
                    : NormalizadorClave.normalizar(celdaIccid.texto());
            if (iccid.isEmpty()) {
                avisos.add(new AvisoParseo(
                        TipoAviso.CLAVE_VACIA_O_MALFORMADA,
                        "",
                        "Fila " + (r + 1) + " de " + nombreOperador + ": ICCID vacío o no legible, se "
                                + "excluyó de la suma."));
                continue;
            }

            Double consumo = leerConsumo(celdaConsumo);
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

    /**
     * Una celda leída del archivo.
     *
     * @param texto  texto de la celda (en .xlsx, tal como lo mostraría
     *               Excel según su formato). Nunca {@code null}.
     * @param numero valor numérico real si la celda es numérica en un
     *               .xlsx (o fórmula con resultado numérico cacheado);
     *               {@code null} en cualquier otro caso, incluido todo
     *               valor leído de un .csv.
     */
    private record Celda(String texto, Double numero) {

        static final Celda VACIA = new Celda("", null);

        static Celda deTexto(String texto) {
            return new Celda(texto == null ? "" : texto, null);
        }

        boolean esNumerica() {
            return numero != null;
        }
    }

    private record Encabezado(int filaIndice, int colIccid, int colConsumo) {
    }

    private Encabezado buscarEncabezado(List<List<Celda>> filas) {
        int limite = Math.min(LIMITE_FILAS_BUSQUEDA_ENCABEZADO, filas.size());
        for (int r = 0; r < limite; r++) {
            List<Celda> fila = filas.get(r);
            Integer colIccid = null;
            Integer colConsumo = null;
            for (int c = 0; c < fila.size(); c++) {
                Optional<String> campo = headerResolver.resolver(fila.get(c).texto());
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

    // ------------------------------------------------------------------
    // .xlsx
    // ------------------------------------------------------------------

    private List<List<Celda>> extraerFilasXlsx(byte[] contenido) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            XSSFSheet hoja = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<List<Celda>> filas = new ArrayList<>();
            int ultimaFila = hoja.getLastRowNum();
            for (int r = 0; r <= ultimaFila; r++) {
                Row fila = hoja.getRow(r);
                List<Celda> celdas = new ArrayList<>();
                if (fila != null) {
                    int ultimaCelda = Math.max(fila.getLastCellNum(), 0);
                    for (int c = 0; c < ultimaCelda; c++) {
                        celdas.add(leerCeldaXlsx(fila.getCell(c), formatter));
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

    /**
     * Lee una celda conservando su valor numérico real. Para fórmulas usa
     * el resultado cacheado (el que Excel guardó al salvar el archivo), no
     * el texto de la fórmula — {@code DataFormatter.formatCellValue} sin
     * evaluador devolvería {@code "SUM(B2:B5)"} en vez del número.
     */
    private Celda leerCeldaXlsx(Cell celda, DataFormatter formatter) {
        if (celda == null) {
            return Celda.VACIA;
        }
        CellType tipo = celda.getCellType() == CellType.FORMULA
                ? celda.getCachedFormulaResultType()
                : celda.getCellType();
        switch (tipo) {
            case NUMERIC: {
                double valor = celda.getNumericCellValue();
                CellStyle estilo = celda.getCellStyle();
                String texto = estilo == null
                        ? String.valueOf(valor)
                        : formatter.formatRawCellContents(valor, estilo.getDataFormat(), estilo.getDataFormatString());
                return new Celda(texto, valor);
            }
            case STRING:
                return Celda.deTexto(celda.getStringCellValue());
            case BOOLEAN:
                return Celda.deTexto(String.valueOf(celda.getBooleanCellValue()));
            default:
                return Celda.VACIA;
        }
    }

    private static boolean esEnteroExacto(double valor) {
        return Math.abs(valor) < MAX_ENTERO_EXACTO && valor == Math.rint(valor);
    }

    // ------------------------------------------------------------------
    // .csv
    // ------------------------------------------------------------------

    private List<List<Celda>> extraerFilasCsv(byte[] contenido) {
        String texto = decodificarTexto(contenido);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }

        List<List<Celda>> filas = new ArrayList<>();
        for (List<String> registro : leerRegistrosCsv(texto)) {
            List<Celda> celdas = new ArrayList<>(registro.size());
            for (String campo : registro) {
                celdas.add(Celda.deTexto(campo));
            }
            filas.add(celdas);
        }
        return filas;
    }

    /**
     * Decodifica como UTF-8 estricto; si hay bytes que no son UTF-8 válido,
     * reintenta como Windows-1252.
     */
    private static String decodificarTexto(byte[] contenido) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(contenido))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(contenido, WINDOWS_1252);
        }
    }

    /**
     * Lector CSV (RFC 4180) con separador coma:
     * <ul>
     *   <li>Un campo que empieza con {@code "} se lee hasta la comilla de
     *       cierre; dentro puede haber comas y saltos de línea, y
     *       {@code ""} representa una comilla literal.</li>
     *   <li>Las comillas simples no son delimitador: se conservan en el
     *       valor y se quitan después con {@link NormalizadorClave}
     *       (caso real de Telcel, spec 02).</li>
     *   <li>Fin de registro: {@code \n}, {@code \r\n} o {@code \r}. Un
     *       salto de línea final no genera un registro vacío extra; una
     *       línea vacía intermedia sí genera un registro con un único
     *       campo vacío.</li>
     *   <li>Una comilla doble abierta que nunca se cierra hace fallar el
     *       parseo de forma explícita, en vez de tragarse en silencio el
     *       resto del archivo dentro de un solo campo.</li>
     * </ul>
     */
    private List<List<String>> leerRegistrosCsv(String texto) {
        List<List<String>> registros = new ArrayList<>();
        List<String> registro = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        boolean registroIniciado = false;
        int lineaInicioComillas = 0;
        int linea = 1;

        int i = 0;
        int n = texto.length();
        while (i < n) {
            char ch = texto.charAt(i);

            if (entreComillas) {
                if (ch == '"') {
                    if (i + 1 < n && texto.charAt(i + 1) == '"') {
                        campo.append('"');
                        i += 2;
                        continue;
                    }
                    entreComillas = false;
                } else {
                    if (ch == '\n') {
                        linea++;
                    }
                    campo.append(ch);
                }
                i++;
                continue;
            }

            switch (ch) {
                case '"':
                    if (campo.length() == 0) {
                        entreComillas = true;
                        lineaInicioComillas = linea;
                    } else {
                        // Comilla en medio de un campo sin comillas: se
                        // trata como carácter literal (lectura tolerante).
                        campo.append(ch);
                    }
                    registroIniciado = true;
                    break;
                case ',':
                    registro.add(campo.toString());
                    campo.setLength(0);
                    registroIniciado = true;
                    break;
                case '\r':
                case '\n':
                    if (ch == '\r' && i + 1 < n && texto.charAt(i + 1) == '\n') {
                        i++;
                    }
                    registro.add(campo.toString());
                    registros.add(registro);
                    registro = new ArrayList<>();
                    campo.setLength(0);
                    registroIniciado = false;
                    linea++;
                    break;
                default:
                    campo.append(ch);
                    registroIniciado = true;
            }
            i++;
        }

        if (entreComillas) {
            throw new ArchivoFormatoInvalidoException(
                    "El archivo .csv de " + nombreOperador + " tiene una comilla doble abierta en la línea "
                            + lineaInicioComillas + " que nunca se cierra.");
        }
        if (registroIniciado) {
            registro.add(campo.toString());
            registros.add(registro);
        }
        return registros;
    }

    // ------------------------------------------------------------------
    // Normalización de valores
    // ------------------------------------------------------------------

    private Celda obtenerCelda(List<Celda> fila, int indice) {
        if (indice < 0 || indice >= fila.size()) {
            return Celda.VACIA;
        }
        Celda celda = fila.get(indice);
        return celda == null ? Celda.VACIA : celda;
    }

    /**
     * Consumo de la celda: el valor numérico real si la celda es numérica
     * (.xlsx), o el texto interpretado con {@link LectorNumero} en
     * cualquier otro caso.
     */
    private Double leerConsumo(Celda celda) {
        if (celda.esNumerica()) {
            return celda.numero();
        }
        return LectorNumero.parsear(celda.texto());
    }
}
