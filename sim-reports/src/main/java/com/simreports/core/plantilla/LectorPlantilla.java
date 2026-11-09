package com.simreports.core.plantilla;

import com.simreports.core.HeaderResolver;
import com.simreports.core.matching.FilaPlantilla;
import com.simreports.core.matching.TablaConFilas;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableColumn;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lee la plantilla de reporte con Apache POI — spec 01 y Fase 2 punto 2
 * (spec 08). Puro: no conoce Spring ni la base de datos.
 *
 * <p>Dos operaciones:</p>
 * <ul>
 *   <li>{@link #detectar(InputStream)}, al subir una plantilla: encuentra y
 *       clasifica las tablas nativas de cada hoja de clientes y valida la
 *       plantilla. Devuelve el {@link MapeoPlantilla} que se persiste.</li>
 *   <li>{@link #leer(InputStream, MapeoPlantilla)}, al generar un reporte: lee
 *       las filas con un mapeo ya guardado (que el usuario pudo corregir a
 *       mano), sin volver a detectar.</li>
 * </ul>
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>Se ignoran las hojas cuyo nombre empieza con {@code BD_} (bases de
 *       origen pegadas en el reporte, spec 09) y las hojas vacías.</li>
 *   <li>Toda otra hoja debe tener al menos una tabla nativa, y ninguna fila de
 *       encabezados de clientes fuera de sus tablas (caso ALSEA bloque 2,
 *       spec 09): si no, la app se saltaría filas en silencio.</li>
 *   <li>Se leen <b>todas</b> las filas de datos de cada tabla, también las que
 *       no tienen línea ni ICCID: el escritor debe poner 0 en ellas (D-015).
 *       Se excluye la fila de totales de la tabla, si la tiene.</li>
 *   <li>Las claves ({@code LINEA}, {@code ICCID}, {@code LINEA_1},
 *       {@code LINEA_2}) y la columna {@code SIM} se leen como texto (spec
 *       01): un número entero se pasa sin decimales. Un ICCID guardado como
 *       número de 16 dígitos o más ya perdió dígitos y es un problema de la
 *       plantilla. El lector no limpia las claves: eso lo hace el motor con
 *       {@code NormalizadorClave}.</li>
 *   <li>Si hay problemas, se lanza una sola {@link PlantillaInvalidaException}
 *       con todos.</li>
 * </ul>
 */
public final class LectorPlantilla {

    private static final Set<String> CAMPOS_CLAVE = Set.of("LINEA", "ICCID", "LINEA_1", "LINEA_2");
    private static final Set<String> CAMPOS_CONSUMO = Set.of("CONSUMO(MB)", "CONSUMO(MB)_1", "CONSUMO(MB)_2");
    private static final List<String> CAMPOS_TEXTO = List.of("LINEA", "ICCID", "LINEA_1", "LINEA_2", "SIM");

    /** Un número de 16 dígitos o más ya no cabe exacto en un {@code double}. */
    private static final double LIMITE_PRECISION_DOUBLE = 1e15;

    /** Máximo de filas que se nombran por problema, para no inundar el mensaje. */
    private static final int MAX_FILAS_EN_MENSAJE = 10;

    private final ResolverTablaPlantilla resolverTabla;
    private final HeaderResolver headerResolver;

    public LectorPlantilla() {
        this.headerResolver = new PlantillaHeaderResolver();
        this.resolverTabla = new ResolverTablaPlantilla(headerResolver);
    }

    /**
     * Detecta, clasifica y valida las tablas de la plantilla.
     *
     * @throws PlantillaInvalidaException con todos los problemas encontrados.
     */
    public MapeoPlantilla detectar(InputStream archivo) {
        Objects.requireNonNull(archivo, "archivo no puede ser null");
        try (XSSFWorkbook libro = abrir(archivo)) {
            List<String> problemas = new ArrayList<>();
            List<MapeoTabla> tablas = new ArrayList<>();
            for (int i = 0; i < libro.getNumberOfSheets(); i++) {
                XSSFSheet hoja = libro.getSheetAt(i);
                if (esHojaIgnorada(hoja)) {
                    continue;
                }
                List<XSSFTable> nativas = hoja.getTables();
                List<Integer> encabezadosSueltos = filasDeEncabezadoFueraDeTablas(hoja, nativas);
                if (nativas.isEmpty()) {
                    problemas.add("Hoja '" + hoja.getSheetName() + "': no tiene ninguna tabla nativa de Excel"
                            + (encabezadosSueltos.isEmpty() ? "" : " (encabezados en la fila "
                            + unir(encabezadosSueltos) + ")")
                            + ". Conviértela a tabla (Insertar → Tabla).");
                    continue;
                }
                for (int fila : encabezadosSueltos) {
                    problemas.add("Hoja '" + hoja.getSheetName() + "': la fila " + fila
                            + " tiene encabezados de clientes fuera de las tablas nativas; sus filas no se"
                            + " leerían. Conviértela en tabla o amplía la tabla existente.");
                }
                for (XSSFTable tabla : nativas) {
                    MapeoTabla mapeo = detectarTabla(hoja, tabla, problemas);
                    if (mapeo != null) {
                        tablas.add(mapeo);
                        leerFilas(hoja, tabla, mapeo, problemas);
                    }
                }
            }
            if (tablas.isEmpty() && problemas.isEmpty()) {
                problemas.add("La plantilla no tiene ninguna hoja de clientes con tabla nativa.");
            }
            lanzarSiHayProblemas(problemas);
            return new MapeoPlantilla(tablas);
        } catch (IOException e) {
            throw new PlantillaInvalidaException(List.of("No se pudo leer el archivo: " + e.getMessage()));
        }
    }

    /**
     * Lee las filas de cada tabla con un mapeo ya guardado.
     *
     * @throws PlantillaInvalidaException si una hoja, tabla o columna del mapeo
     *         ya no existe en el archivo, o si un ICCID está guardado como número.
     */
    public PlantillaLeida leer(InputStream archivo, MapeoPlantilla mapeo) {
        Objects.requireNonNull(archivo, "archivo no puede ser null");
        Objects.requireNonNull(mapeo, "mapeo no puede ser null");
        try (XSSFWorkbook libro = abrir(archivo)) {
            List<String> problemas = new ArrayList<>();
            List<TablaLeida> leidas = new ArrayList<>();
            for (MapeoTabla mapeoTabla : mapeo.tablas()) {
                XSSFSheet hoja = libro.getSheet(mapeoTabla.hoja());
                XSSFTable tabla = hoja == null ? null : buscarTabla(hoja, mapeoTabla.nombreTabla());
                if (tabla == null) {
                    problemas.add("Tabla '" + mapeoTabla.identificador() + "': ya no existe en el archivo"
                            + (hoja == null ? " (no está la hoja)" : "") + ".");
                    continue;
                }
                TablaLeida leida = leerFilas(hoja, tabla, mapeoTabla, problemas);
                if (leida != null) {
                    leidas.add(leida);
                }
            }
            lanzarSiHayProblemas(problemas);
            return new PlantillaLeida(leidas);
        } catch (IOException e) {
            throw new PlantillaInvalidaException(List.of("No se pudo leer el archivo: " + e.getMessage()));
        }
    }

    // ---------- Detección ----------

    private MapeoTabla detectarTabla(XSSFSheet hoja, XSSFTable tabla, List<String> problemas) {
        String id = "Hoja '" + hoja.getSheetName() + "', tabla '" + tabla.getName() + "'";
        if (tabla.getHeaderRowCount() < 1) {
            problemas.add(id + ": no tiene fila de encabezados.");
            return null;
        }
        List<String> encabezados = new ArrayList<>();
        for (XSSFTableColumn columna : tabla.getColumns()) {
            encabezados.add(columna.getName());
        }
        try {
            TablaPlantillaResuelta resuelta = resolverTabla.resolver(encabezados);
            return new MapeoTabla(hoja.getSheetName(), tabla.getName(), tabla.getArea().formatAsString(),
                    resuelta.tipo(), resuelta.columnaPorCampoLogico(), resuelta.encabezadosNoReconocidos());
        } catch (TablaPlantillaNoReconocidaException e) {
            problemas.add(id + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Filas (1 = primera) con encabezados de clientes fuera de toda tabla
     * nativa: al menos un campo clave y un campo de consumo en celdas que no
     * pertenecen a ninguna tabla.
     */
    private List<Integer> filasDeEncabezadoFueraDeTablas(XSSFSheet hoja, List<XSSFTable> tablas) {
        List<Integer> filas = new ArrayList<>();
        for (Row fila : hoja) {
            boolean clave = false;
            boolean consumo = false;
            for (Cell celda : fila) {
                if (celda.getCellType() != CellType.STRING || dentroDeAlgunaTabla(celda, tablas)) {
                    continue;
                }
                String campo = headerResolver.resolver(celda.getStringCellValue()).orElse("");
                clave |= CAMPOS_CLAVE.contains(campo);
                consumo |= CAMPOS_CONSUMO.contains(campo);
            }
            if (clave && consumo) {
                filas.add(fila.getRowNum() + 1);
            }
        }
        return filas;
    }

    private static boolean dentroDeAlgunaTabla(Cell celda, List<XSSFTable> tablas) {
        for (XSSFTable tabla : tablas) {
            if (celda.getRowIndex() >= tabla.getStartRowIndex() && celda.getRowIndex() <= tabla.getEndRowIndex()
                    && celda.getColumnIndex() >= tabla.getStartColIndex()
                    && celda.getColumnIndex() <= tabla.getEndColIndex()) {
                return true;
            }
        }
        return false;
    }

    // ---------- Lectura de filas ----------

    /** Lee las filas de datos con el mapeo dado; {@code null} si falta alguna columna. */
    private TablaLeida leerFilas(XSSFSheet hoja, XSSFTable tabla, MapeoTabla mapeo, List<String> problemas) {
        String id = "Hoja '" + hoja.getSheetName() + "', tabla '" + tabla.getName() + "'";
        Map<String, Integer> indices = indicesDeColumna(tabla, mapeo, id, problemas);
        if (indices == null) {
            return null;
        }
        int primera = tabla.getStartRowIndex() + tabla.getHeaderRowCount();
        int ultima = tabla.getEndRowIndex() - tabla.getTotalsRowCount();
        List<FilaPlantilla> filas = new ArrayList<>();
        Set<Integer> iccidNumerico = new TreeSet<>();
        for (int r = primera; r <= ultima; r++) {
            Row fila = hoja.getRow(r);
            Map<String, String> valores = new LinkedHashMap<>();
            for (String campo : CAMPOS_TEXTO) {
                Integer indice = indices.get(campo);
                Cell celda = (fila == null || indice == null) ? null : fila.getCell(indice);
                if ("ICCID".equals(campo) && esNumeroSinPrecision(celda)) {
                    iccidNumerico.add(r + 1);
                    valores.put(campo, null);
                } else {
                    valores.put(campo, texto(celda));
                }
            }
            filas.add(new FilaPlantilla(valores.get("LINEA"), valores.get("ICCID"), valores.get("LINEA_1"),
                    valores.get("LINEA_2"), valores.get("SIM"), r + 1));
        }
        if (!iccidNumerico.isEmpty()) {
            problemas.add(id + ": ICCID guardado como número (perdió los últimos dígitos) en la fila "
                    + unir(new ArrayList<>(iccidNumerico)) + ". Dale formato de texto a la columna ICCID"
                    + " y vuelve a escribir esos ICCID.");
        }
        return new TablaLeida(mapeo, new TablaConFilas(hoja.getSheetName(), mapeo.identificador(), mapeo.tipo(), filas),
                indices);
    }

    /** Campo lógico → índice de columna (0 = A); {@code null} si una columna del mapeo no existe. */
    private static Map<String, Integer> indicesDeColumna(
            XSSFTable tabla, MapeoTabla mapeo, String id, List<String> problemas) {
        Map<String, Integer> porEncabezado = new LinkedHashMap<>();
        for (XSSFTableColumn columna : tabla.getColumns()) {
            porEncabezado.putIfAbsent(columna.getName(), tabla.getStartColIndex() + columna.getColumnIndex());
        }
        Map<String, Integer> indices = new LinkedHashMap<>();
        List<String> faltantes = new ArrayList<>();
        for (Map.Entry<String, String> e : mapeo.columnaPorCampoLogico().entrySet()) {
            Integer indice = porEncabezado.get(e.getValue());
            if (indice == null) {
                faltantes.add("'" + e.getValue() + "' (" + e.getKey() + ")");
            } else {
                indices.put(e.getKey(), indice);
            }
        }
        if (!faltantes.isEmpty()) {
            problemas.add(id + ": ya no tiene la columna " + String.join(", ", faltantes)
                    + ". Corrige el mapeo o vuelve a subir la plantilla.");
            return null;
        }
        return indices;
    }

    // ---------- Celdas ----------

    /**
     * Valor de una celda clave como texto (spec 01): texto tal cual; número
     * entero sin decimales ni notación científica; lo demás, {@code null}.
     */
    static String texto(Cell celda) {
        if (celda == null) {
            return null;
        }
        CellType tipo = celda.getCellType() == CellType.FORMULA ? celda.getCachedFormulaResultType() : celda.getCellType();
        return switch (tipo) {
            case STRING -> {
                String valor = celda.getStringCellValue();
                yield valor.isBlank() ? null : valor;
            }
            case NUMERIC -> {
                BigDecimal numero = BigDecimal.valueOf(celda.getNumericCellValue()).stripTrailingZeros();
                yield numero.scale() <= 0 ? numero.toBigInteger().toString() : numero.toPlainString();
            }
            default -> null;
        };
    }

    private static boolean esNumeroSinPrecision(Cell celda) {
        if (celda == null) {
            return false;
        }
        CellType tipo = celda.getCellType() == CellType.FORMULA ? celda.getCachedFormulaResultType() : celda.getCellType();
        return tipo == CellType.NUMERIC && Math.abs(celda.getNumericCellValue()) >= LIMITE_PRECISION_DOUBLE;
    }

    // ---------- Utilidades ----------

    private static XSSFWorkbook abrir(InputStream archivo) throws IOException {
        try {
            return new XSSFWorkbook(archivo);
        } catch (RuntimeException e) {
            // POI lanza NotOfficeXmlFileException, POIXMLException, etc. ante un archivo que no es .xlsx.
            throw new PlantillaInvalidaException(List.of(
                    "El archivo no es un libro de Excel .xlsx válido (" + e.getClass().getSimpleName() + ")."));
        }
    }

    private static boolean esHojaIgnorada(XSSFSheet hoja) {
        if (hoja.getSheetName().strip().toUpperCase(Locale.ROOT).startsWith("BD_")) {
            return true;
        }
        for (Row fila : hoja) {
            for (Cell celda : fila) {
                if (celda.getCellType() != CellType.BLANK) {
                    return false;
                }
            }
        }
        return true;
    }

    private static XSSFTable buscarTabla(XSSFSheet hoja, String nombre) {
        for (XSSFTable tabla : hoja.getTables()) {
            if (tabla.getName().equals(nombre)) {
                return tabla;
            }
        }
        return null;
    }

    private static String unir(List<Integer> filas) {
        List<String> partes = new ArrayList<>();
        for (int i = 0; i < Math.min(filas.size(), MAX_FILAS_EN_MENSAJE); i++) {
            partes.add(String.valueOf(filas.get(i)));
        }
        String texto = String.join(", ", partes);
        if (filas.size() > MAX_FILAS_EN_MENSAJE) {
            texto += " y " + (filas.size() - MAX_FILAS_EN_MENSAJE) + " más";
        }
        return texto;
    }

    private static void lanzarSiHayProblemas(List<String> problemas) {
        if (!problemas.isEmpty()) {
            throw new PlantillaInvalidaException(problemas);
        }
    }
}
