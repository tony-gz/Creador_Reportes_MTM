package com.simreports.core.parsers;

import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.AvisoParseo;
import com.simreports.core.HeaderResolver;
import com.simreports.core.OrigenParser;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableColumn;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Parser del archivo de Alestra — ver spec 02, sección "Alestra".
 *
 * <p>El archivo real siempre trae tabla nativa de Excel (confirmado en
 * Fase 0), aunque tenga filas de metadata antes de la tabla — por eso el
 * parser usa {@code XSSFSheet.getTables()} en vez de asumir que el
 * encabezado está en la fila 1. Si algún mes llegara sin tabla nativa, se
 * lanza {@link ArchivoFormatoInvalidoException} en vez de adivinar dónde
 * empieza el encabezado (misma filosofía que el resolver de la
 * plantilla, spec 01).</p>
 *
 * <p>Duplicados: si una línea aparece más de una vez, se agrega
 * (suma) — nunca se queda con el primer o el último valor. Cada
 * duplicado detectado genera un {@link AvisoParseo}, igual que cada
 * línea vacía/no legible, que se excluye de la suma en vez de romper el
 * proceso completo.</p>
 */
public class AlestraParser implements OrigenParser {

    private static final String CAMPO_LINEA = "LINEA";
    private static final String CAMPO_CONSUMO = "CONSUMO";

    private final HeaderResolver headerResolver;

    public AlestraParser() {
        this(new AlestraHeaderResolver());
    }

    public AlestraParser(HeaderResolver headerResolver) {
        this.headerResolver = Objects.requireNonNull(headerResolver, "headerResolver no puede ser null");
    }

    @Override
    public ResultadoParseo parsear(InputStream archivo) {
        Objects.requireNonNull(archivo, "archivo no puede ser null");

        try (XSSFWorkbook workbook = new XSSFWorkbook(archivo)) {
            TablaEncontrada encontrada = buscarPrimeraTabla(workbook);
            return procesarTabla(encontrada.hoja(), encontrada.tabla());
        } catch (IOException e) {
            throw new ArchivoFormatoInvalidoException(
                    "No se pudo leer el archivo de Alestra como .xlsx: " + e.getMessage(), e);
        }
    }

    private record TablaEncontrada(XSSFSheet hoja, XSSFTable tabla) {
    }

    private TablaEncontrada buscarPrimeraTabla(XSSFWorkbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            XSSFSheet hoja = workbook.getSheetAt(i);
            List<XSSFTable> tablas = hoja.getTables();
            if (!tablas.isEmpty()) {
                return new TablaEncontrada(hoja, tablas.get(0));
            }
        }
        throw new ArchivoFormatoInvalidoException(
                "El archivo de Alestra no tiene ninguna tabla nativa de Excel (Table). Se esperaba una "
                        + "tabla con columnas LINEA y Tráfico MB.");
    }

    private ResultadoParseo procesarTabla(XSSFSheet hoja, XSSFTable tabla) {
        List<XSSFTableColumn> columnas = tabla.getColumns();
        int columnaInicio = tabla.getStartColIndex();

        Map<String, Integer> indicePorCampoLogico = new LinkedHashMap<>();
        List<String> encabezadosEncontrados = new ArrayList<>();

        for (int i = 0; i < columnas.size(); i++) {
            String nombreReal = columnas.get(i).getName();
            encabezadosEncontrados.add(nombreReal);
            Optional<String> campo = headerResolver.resolver(nombreReal);
            if (campo.isPresent()) {
                indicePorCampoLogico.putIfAbsent(campo.get(), columnaInicio + i);
            }
        }

        if (!indicePorCampoLogico.containsKey(CAMPO_LINEA) || !indicePorCampoLogico.containsKey(CAMPO_CONSUMO)) {
            throw new ArchivoFormatoInvalidoException(
                    "No se encontraron las columnas obligatorias LINEA y/o Tráfico MB en la tabla de "
                            + "Alestra. Encabezados encontrados: " + encabezadosEncontrados);
        }

        int colLinea = indicePorCampoLogico.get(CAMPO_LINEA);
        int colConsumo = indicePorCampoLogico.get(CAMPO_CONSUMO);

        DataFormatter formatter = new DataFormatter();
        Map<String, List<Double>> valoresPorLinea = new LinkedHashMap<>();
        List<AvisoParseo> avisos = new ArrayList<>();

        // El rango de la tabla nativa incluye la fila de totales si la
        // tabla la tiene activada ("Fila de totales" en Excel); esa fila no
        // es una línea real y, si se leyera, sumaría el total completo como
        // si fuera el consumo de una línea llamada "Total".
        int primeraFilaDatos = tabla.getStartRowIndex() + 1;
        int ultimaFilaDatos = tabla.getEndRowIndex() - Math.max(tabla.getTotalsRowCount(), 0);

        for (int r = primeraFilaDatos; r <= ultimaFilaDatos; r++) {
            Row fila = hoja.getRow(r);
            if (fila == null) {
                continue;
            }

            String linea = formatter.formatCellValue(fila.getCell(colLinea)).trim();
            if (linea.isEmpty()) {
                avisos.add(new AvisoParseo(
                        TipoAviso.CLAVE_VACIA_O_MALFORMADA,
                        "",
                        "Fila " + (r + 1) + " de Alestra: LINEA vacía, se excluyó de la suma."));
                continue;
            }

            Double consumo = leerComoNumero(fila.getCell(colConsumo));
            if (consumo == null) {
                avisos.add(new AvisoParseo(
                        TipoAviso.CLAVE_VACIA_O_MALFORMADA,
                        linea,
                        "Fila " + (r + 1) + " de Alestra: Tráfico MB no es un valor numérico legible, "
                                + "se excluyó de la suma."));
                continue;
            }

            valoresPorLinea.computeIfAbsent(linea, k -> new ArrayList<>()).add(consumo);
        }

        Map<String, Double> consumos = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : valoresPorLinea.entrySet()) {
            List<Double> valores = entry.getValue();
            double suma = valores.stream().mapToDouble(Double::doubleValue).sum();
            consumos.put(entry.getKey(), suma);
            if (valores.size() > 1) {
                avisos.add(new AvisoParseo(
                        TipoAviso.CLAVE_DUPLICADA,
                        entry.getKey(),
                        "La línea apareció " + valores.size() + " veces en el archivo de Alestra; se "
                                + "sumaron los MB (total: " + suma + " MB)."));
            }
        }

        return new ResultadoParseo(consumos, avisos);
    }

    /**
     * Lee una celda de consumo como número. Acepta celdas numéricas
     * (caso esperado), fórmulas cuyo resultado cacheado sea numérico, y
     * texto que en realidad representa un número (defensivo — el
     * archivo real no debería traer esto, pero no cuesta tolerarlo).
     * Cualquier otro caso (celda nula, texto no numérico) devuelve
     * {@code null} para que la fila se excluya con un aviso, en vez de
     * lanzar una excepción cruda de Apache POI.
     */
    private Double leerComoNumero(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.FORMULA
                    && cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.STRING) {
                String texto = cell.getStringCellValue().trim().replace(",", "");
                if (texto.isEmpty()) {
                    return null;
                }
                return Double.parseDouble(texto);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }
}
