package com.simreports.core.reporte;

import com.simreports.core.matching.FilaPlantilla;
import com.simreports.core.matching.ResultadoFilaPlantilla;
import com.simreports.core.matching.ResultadoTabla;
import com.simreports.core.plantilla.PlantillaLeida;
import com.simreports.core.plantilla.TablaLeida;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Escribe el reporte de consumos sobre una copia de la plantilla — Fase 2
 * punto 4 (spec 08). Puro: no conoce Spring.
 *
 * <ul>
 *   <li>Cada celda de consumo de cada fila de cada tabla recibe lo que asignó
 *       el motor, como número: {@code CONSUMO(MB)} en las tablas simples,
 *       {@code CONSUMO(MB)_1} y {@code CONSUMO(MB)_2} en las dual. Reemplaza
 *       lo que hubiera: la fórmula {@code BUSCARV}, un valor escrito a mano
 *       o un texto. Sin coincidencia se escribe 0, aunque la celda tuviera un
 *       valor fijo, porque ese valor es del mes anterior (D-015). El valor ya
 *       viene redondeado a dos decimales (D-022) y la celda queda con formato
 *       {@value #FORMATO_CONSUMO}; el resto del estilo se conserva.</li>
 *   <li>Lo que está fuera de las tablas no se toca (p. ej. el bloque
 *       STARLINK de METROBÚS, D-017).</li>
 *   <li>Se quitan las hojas {@code BD_*}: traen datos del mes anterior y,
 *       con los valores ya escritos, ninguna fórmula las usa (decisión del
 *       2026-11-09).</li>
 *   <li>Se recalculan las fórmulas que quedan (los {@code CONSUMO_TOTAL}) y
 *       el libro queda marcado para que Excel recalcule al abrirlo.</li>
 * </ul>
 *
 * <p>{@link #sumarConsumos} relee el archivo escrito para la verificación
 * independiente ({@code VerificadorReporteEscrito}).</p>
 */
public final class EscritorReporte {

    /** Formato de número de toda celda de consumo escrita (D-022). */
    static final String FORMATO_CONSUMO = "#,##0.00";

    /**
     * @param plantilla  el .xlsx de la versión de plantilla usada.
     * @param leida      sus tablas leídas con el mapeo guardado.
     * @param resultados lo que devolvió el motor, una por tabla y en el mismo
     *                   orden que {@code leida.tablas()}.
     */
    public ReporteEscrito escribir(byte[] plantilla, PlantillaLeida leida, List<ResultadoTabla> resultados) {
        Objects.requireNonNull(plantilla, "plantilla no puede ser null");
        Objects.requireNonNull(leida, "leida no puede ser null");
        Objects.requireNonNull(resultados, "resultados no puede ser null");
        if (resultados.size() != leida.tablas().size()) {
            throw new IllegalArgumentException("Hay " + leida.tablas().size() + " tablas y " + resultados.size()
                    + " resultados");
        }
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(plantilla));
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            int celdas = 0;
            EstilosConsumo estilos = new EstilosConsumo(libro);
            for (int t = 0; t < resultados.size(); t++) {
                celdas += escribirTabla(libro, leida.tablas().get(t), resultados.get(t), estilos);
            }
            quitarHojasBd(libro);
            int sinRecalcular = recalcularFormulas(libro);
            libro.setForceFormulaRecalculation(true);
            libro.write(salida);
            return new ReporteEscrito(salida.toByteArray(), celdas, sinRecalcular);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el reporte", e);
        }
    }

    /**
     * Suma de las celdas de consumo ({@code CONSUMO(MB)}, {@code _1} y
     * {@code _2}, no {@code CONSUMO_TOTAL}) releídas de un reporte ya escrito.
     * Una celda que no es número cuenta 0, así que se nota como descuadre.
     */
    public static double sumarConsumos(byte[] reporte, PlantillaLeida leida) {
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(reporte))) {
            double total = 0.0;
            for (TablaLeida tabla : leida.tablas()) {
                Sheet hoja = libro.getSheet(tabla.mapeo().hoja());
                for (String campo : List.of("CONSUMO(MB)", "CONSUMO(MB)_1", "CONSUMO(MB)_2")) {
                    Integer columna = tabla.indiceColumnaPorCampoLogico().get(campo);
                    if (columna == null || hoja == null) {
                        continue;
                    }
                    for (FilaPlantilla fila : tabla.tablaConFilas().filas()) {
                        Row row = hoja.getRow(fila.filaExcel() - 1);
                        Cell celda = row == null ? null : row.getCell(columna);
                        if (celda != null && celda.getCellType() == CellType.NUMERIC) {
                            total += celda.getNumericCellValue();
                        }
                    }
                }
            }
            return total;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo releer el reporte escrito", e);
        }
    }

    private static int escribirTabla(XSSFWorkbook libro, TablaLeida tabla, ResultadoTabla resultado,
                                     EstilosConsumo estilos) {
        XSSFSheet hoja = libro.getSheet(tabla.mapeo().hoja());
        List<FilaPlantilla> filas = tabla.tablaConFilas().filas();
        if (hoja == null || resultado.filas().size() != filas.size()) {
            throw new IllegalArgumentException("El resultado de '" + tabla.mapeo().identificador()
                    + "' no corresponde a la plantilla");
        }
        int celdas = 0;
        for (int i = 0; i < filas.size(); i++) {
            int fila = filas.get(i).filaExcel() - 1;
            ResultadoFilaPlantilla r = resultado.filas().get(i);
            if (r.consumo() != null) {
                escribir(hoja, estilos, fila, tabla.indiceColumnaPorCampoLogico().get("CONSUMO(MB)"), r.consumo().consumoMb());
                celdas++;
            } else {
                escribir(hoja, estilos, fila, tabla.indiceColumnaPorCampoLogico().get("CONSUMO(MB)_1"), r.consumo1().consumoMb());
                escribir(hoja, estilos, fila, tabla.indiceColumnaPorCampoLogico().get("CONSUMO(MB)_2"), r.consumo2().consumoMb());
                celdas += 2;
            }
        }
        return celdas;
    }

    private static void escribir(XSSFSheet hoja, EstilosConsumo estilos, int fila, Integer columna, double valor) {
        Objects.requireNonNull(columna, "la tabla no tiene columna de consumo");
        Row row = hoja.getRow(fila) == null ? hoja.createRow(fila) : hoja.getRow(fila);
        Cell celda = row.getCell(columna) == null ? row.createCell(columna) : row.getCell(columna);
        if (celda.getCellType() == CellType.FORMULA) {
            celda.removeFormula();
        }
        celda.setCellValue(valor);
        celda.setCellStyle(estilos.conDosDecimales(celda.getCellStyle()));
    }

    /**
     * Da a cada estilo de celda de consumo el formato {@value #FORMATO_CONSUMO}
     * sin perder lo demás (fuente, bordes, relleno). Reutiliza una copia por
     * estilo original para no multiplicar estilos en el libro.
     */
    private static final class EstilosConsumo {
        private final XSSFWorkbook libro;
        private final short formato;
        private final Map<Integer, CellStyle> copias = new HashMap<>();

        EstilosConsumo(XSSFWorkbook libro) {
            this.libro = libro;
            this.formato = libro.createDataFormat().getFormat(FORMATO_CONSUMO);
        }

        CellStyle conDosDecimales(CellStyle original) {
            if (FORMATO_CONSUMO.equals(original.getDataFormatString())) {
                return original;
            }
            return copias.computeIfAbsent((int) original.getIndex(), indice -> {
                CellStyle copia = libro.createCellStyle();
                copia.cloneStyleFrom(original);
                copia.setDataFormat(formato);
                return copia;
            });
        }
    }

    private static void quitarHojasBd(XSSFWorkbook libro) {
        String activa = libro.getSheetName(libro.getActiveSheetIndex());
        for (int i = libro.getNumberOfSheets() - 1; i >= 0; i--) {
            if (libro.getSheetName(i).strip().toUpperCase(Locale.ROOT).startsWith("BD_")) {
                libro.removeSheetAt(i);
            }
        }
        int indice = Math.max(libro.getSheetIndex(activa), 0);
        libro.setActiveSheet(indice);
        libro.setSelectedTab(indice);
    }

    private static int recalcularFormulas(XSSFWorkbook libro) {
        FormulaEvaluator evaluador = libro.getCreationHelper().createFormulaEvaluator();
        int fallidas = 0;
        for (Sheet hoja : libro) {
            for (Row fila : hoja) {
                for (Cell celda : fila) {
                    if (celda.getCellType() != CellType.FORMULA) {
                        continue;
                    }
                    try {
                        evaluador.evaluateFormulaCell(celda);
                    } catch (RuntimeException e) {
                        fallidas++;
                    }
                }
            }
        }
        return fallidas;
    }
}
