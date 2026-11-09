package com.simreports.core.plantilla;

import com.simreports.core.matching.FilaPlantilla;
import com.simreports.core.matching.TablaConFilas;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LectorPlantillaTest {

    private final LectorPlantilla lector = new LectorPlantilla();

    // ---------- Utilidades para armar libros de prueba ----------

    /** Escribe una fila; los {@code Double} van como número, lo demás como texto, {@code null} = sin celda. */
    private static void fila(XSSFSheet hoja, int indice, int columnaInicial, Object... valores) {
        Row row = hoja.getRow(indice) == null ? hoja.createRow(indice) : hoja.getRow(indice);
        for (int i = 0; i < valores.length; i++) {
            Object v = valores[i];
            if (v == null) {
                continue;
            }
            Cell celda = row.createCell(columnaInicial + i);
            if (v instanceof Double d) {
                celda.setCellValue(d);
            } else {
                celda.setCellValue(v.toString());
            }
        }
    }

    /** Tabla nativa sobre un rango ya escrito (la fila superior es el encabezado). */
    private static XSSFTable tabla(XSSFSheet hoja, String nombre, int filaIni, int colIni, int filaFin, int colFin) {
        XSSFTable tabla = hoja.createTable(new AreaReference(new CellReference(filaIni, colIni),
                new CellReference(filaFin, colFin), SpreadsheetVersion.EXCEL2007));
        tabla.setName(nombre);
        tabla.setDisplayName(nombre);
        tabla.updateHeaders();
        return tabla;
    }

    /** Hoja con una tabla cascada SIM/LINEA/ICCID/CONSUMO(MB) en A6, con las filas dadas. */
    private static XSSFSheet hojaCascada(XSSFWorkbook libro, String nombreHoja, String nombreTabla, Object[]... filas) {
        XSSFSheet hoja = libro.createSheet(nombreHoja);
        fila(hoja, 0, 0, "Título del cliente");
        fila(hoja, 5, 0, "SIM", "LINEA", "ICCID", "UNIDAD", "CONSUMO(MB)");
        for (int i = 0; i < filas.length; i++) {
            fila(hoja, 6 + i, 0, filas[i]);
        }
        tabla(hoja, nombreTabla, 5, 0, 5 + Math.max(filas.length, 1), 4);
        return hoja;
    }

    private static InputStream bytes(XSSFWorkbook libro) {
        try (libro; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            libro.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] contenido(XSSFWorkbook libro) {
        try {
            return bytes(libro).readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private PlantillaLeida detectarYLeer(byte[] archivo) {
        MapeoPlantilla mapeo = lector.detectar(new ByteArrayInputStream(archivo));
        return lector.leer(new ByteArrayInputStream(archivo), mapeo);
    }

    private static List<String> problemas(Runnable accion) {
        return assertThrows(PlantillaInvalidaException.class, accion::run).getProblemas();
    }

    // ---------- Detección y lectura ----------

    @Test
    void detectaUnaTablaCascadaYLeeSusFilasConFilaDeExcelYOperador() {
        XSSFWorkbook libro = new XSSFWorkbook();
        hojaCascada(libro, "GEPP", "Tabla3",
                new Object[]{"ALESTRA", "5511112222", "8952020000000000001F", "Unidad 1", "0"},
                new Object[]{"TELCEL", "pendiente", "89520700000000000011", "Unidad 2", "0"});
        byte[] archivo = contenido(libro);

        MapeoPlantilla mapeo = lector.detectar(new ByteArrayInputStream(archivo));

        MapeoTabla mt = mapeo.tablas().get(0);
        assertEquals(new MapeoTabla("GEPP", "Tabla3", "A6:E8", TipoTablaPlantilla.SIMPLE_CASCADA,
                Map.of("SIM", "SIM", "LINEA", "LINEA", "ICCID", "ICCID", "CONSUMO(MB)", "CONSUMO(MB)"),
                List.of("UNIDAD")), mt);

        TablaLeida leida = lector.leer(new ByteArrayInputStream(archivo), mapeo).tablas().get(0);
        TablaConFilas tcf = leida.tablaConFilas();
        assertEquals("GEPP", tcf.hoja());
        assertEquals("GEPP · Tabla3", tcf.identificador());
        assertEquals(TipoTablaPlantilla.SIMPLE_CASCADA, tcf.tipo());
        assertEquals(List.of(
                new FilaPlantilla("5511112222", "8952020000000000001F", null, null, "ALESTRA", 7),
                new FilaPlantilla("pendiente", "89520700000000000011", null, null, "TELCEL", 8)), tcf.filas());
        assertEquals(4, leida.indiceColumnaPorCampoLogico().get("CONSUMO(MB)"));
    }

    @Test
    void lineaGuardadaComoNumeroSeLeeSinDecimalesNiNotacionCientifica() {
        XSSFWorkbook libro = new XSSFWorkbook();
        hojaCascada(libro, "GEPP", "Tabla3", new Object[]{"ALESTRA", 5511112222.0, null, "Unidad", 0.0});

        FilaPlantilla fila = detectarYLeer(contenido(libro)).tablas().get(0).tablaConFilas().filas().get(0);

        assertEquals("5511112222", fila.linea());
        assertNull(fila.iccid());
    }

    @Test
    void leeElResultadoGuardadoDeUnaFormulaComoTexto() {
        XSSFWorkbook libro = new XSSFWorkbook();
        XSSFSheet hoja = hojaCascada(libro, "GEPP", "Tabla3", new Object[]{"ALESTRA", null, null, "Unidad", 0.0});
        Cell celda = hoja.getRow(6).createCell(1);
        celda.setCellFormula("\"55\"&\"11112222\"");
        libro.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(celda);

        assertEquals("5511112222",
                detectarYLeer(contenido(libro)).tablas().get(0).tablaConFilas().filas().get(0).linea());
    }

    @Test
    void leeTodasLasFilasAunqueNoTenganLineaNiIccid() {
        // D-015: el escritor pone 0 también en esas filas, así que deben llegar al motor.
        XSSFWorkbook libro = new XSSFWorkbook();
        hojaCascada(libro, "GEPP", "Tabla3",
                new Object[]{"ALESTRA", "5511112222", null, "Unidad", 0.0},
                new Object[]{null, null, null, "Dado de baja", "NA"},
                new Object[]{"ALESTRA", "5511113333", null, "Unidad", 0.0});

        List<FilaPlantilla> filas = detectarYLeer(contenido(libro)).tablas().get(0).tablaConFilas().filas();

        assertEquals(3, filas.size());
        assertEquals(new FilaPlantilla(null, null, null, null, null, 8), filas.get(1));
    }

    @Test
    void excluyeLaFilaDeTotalesDeLaTabla() {
        XSSFWorkbook libro = new XSSFWorkbook();
        XSSFSheet hoja = hojaCascada(libro, "GEPP", "Tabla3",
                new Object[]{"ALESTRA", "5511112222", null, "Unidad", 0.0},
                new Object[]{null, "Total", null, null, 0.0});
        hoja.getTables().get(0).getCTTable().setTotalsRowCount(1);

        List<FilaPlantilla> filas = detectarYLeer(contenido(libro)).tablas().get(0).tablaConFilas().filas();

        assertEquals(1, filas.size());
        assertEquals("5511112222", filas.get(0).linea());
    }

    @Test
    void ignoraLasHojasBdYLasHojasVacias() {
        XSSFWorkbook libro = new XSSFWorkbook();
        hojaCascada(libro, "GEPP", "Tabla3", new Object[]{"ALESTRA", "5511112222", null, "Unidad", 0.0});
        XSSFSheet bd = libro.createSheet("BD_ATT");
        fila(bd, 0, 0, "ICCID", "Uso de ciclo hasta la fecha (MB)");
        fila(bd, 1, 0, "89520700000000000011", 5.0);
        tabla(bd, "Tabla6", 0, 0, 1, 1); // sin columna de consumo de plantilla: no se podría clasificar
        libro.createSheet("Hoja vacía");

        PlantillaLeida leida = detectarYLeer(contenido(libro));

        assertEquals(1, leida.tablas().size());
        assertEquals("GEPP", leida.tablas().get(0).mapeo().hoja());
    }

    @Test
    void dosTablasDeLaMismaHojaYUnaDualSimConSusColumnas() {
        XSSFWorkbook libro = new XSSFWorkbook();
        XSSFSheet alsea = hojaCascada(libro, "ALSEA", "Tabla2", new Object[]{"ALESTRA", "5511112222", null, "U", 0.0});
        fila(alsea, 20, 0, "SIM", "LINEA", "ICCID", "UNIDAD", "CONSUMO (MB)");
        fila(alsea, 21, 0, "Multicarrier", "5511113333", null, "U", 0.0);
        tabla(alsea, "Tabla20", 20, 0, 21, 4);
        XSSFSheet bimbo = libro.createSheet("BIMBO");
        fila(bimbo, 10, 1, "SIM", "LINEA_1", "LINEA_2", "CONSUMO(MB)_1", "CONSUMO(MB)_2", "CONSUMO_TOTAL");
        fila(bimbo, 11, 1, "ALESTRA", "5511110001", "5511110002", 0.0, 0.0, 0.0);
        tabla(bimbo, "Tabla1", 10, 1, 11, 6);

        PlantillaLeida leida = detectarYLeer(contenido(libro));

        assertEquals(List.of("ALSEA · Tabla2", "ALSEA · Tabla20", "BIMBO · Tabla1"),
                leida.tablas().stream().map(t -> t.mapeo().identificador()).toList());
        assertEquals("ALSEA", leida.tablas().get(1).tablaConFilas().hoja());
        TablaLeida dual = leida.tablas().get(2);
        assertEquals(TipoTablaPlantilla.DUAL_SIM, dual.mapeo().tipo());
        assertEquals(FilaPlantilla.dualSim("5511110001", "5511110002").conSim("ALESTRA").enFila(12),
                dual.tablaConFilas().filas().get(0));
        assertEquals(4, dual.indiceColumnaPorCampoLogico().get("CONSUMO(MB)_1"));
        assertEquals(5, dual.indiceColumnaPorCampoLogico().get("CONSUMO(MB)_2"));
        assertEquals(3, leida.tablasConFilas().size());
    }

    // ---------- Problemas de la plantilla ----------

    @Test
    void hojaDeClientesSinTablaNativaEsUnProblemaConLaFilaDeEncabezados() {
        XSSFWorkbook libro = new XSSFWorkbook();
        hojaCascada(libro, "GEPP", "Tabla3", new Object[]{"ALESTRA", "5511112222", null, "U", 0.0});
        XSSFSheet dentalia = libro.createSheet("DENTALIA");
        fila(dentalia, 5, 0, "SIM", "LINEA", "ICCID", "CONSUMO(MB)");
        fila(dentalia, 6, 0, "AT&T", null, "89520700000000000011", 0.0);
        byte[] archivo = contenido(libro);

        List<String> problemas = problemas(() -> lector.detectar(new ByteArrayInputStream(archivo)));

        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("Hoja 'DENTALIA': no tiene ninguna tabla nativa"), problemas.get(0));
        assertTrue(problemas.get(0).contains("encabezados en la fila 6"), problemas.get(0));
    }

    @Test
    void encabezadosFueraDeLasTablasSonUnProblema() {
        // ALSEA real: una tabla nativa arriba y un segundo bloque suelto abajo.
        XSSFWorkbook libro = new XSSFWorkbook();
        XSSFSheet alsea = hojaCascada(libro, "ALSEA", "Tabla2", new Object[]{"ALESTRA", "5511112222", null, "U", 0.0});
        fila(alsea, 35, 0, "SIM", "LINEA", "ICCID", "UNIDAD", "CONSUMO(MB)");
        fila(alsea, 36, 0, "ALESTRA", "5511113333", null, "U", 0.0);
        byte[] archivo = contenido(libro);

        List<String> problemas = problemas(() -> lector.detectar(new ByteArrayInputStream(archivo)));

        assertEquals(List.of("Hoja 'ALSEA': la fila 36 tiene encabezados de clientes fuera de las tablas nativas;"
                + " sus filas no se leerían. Conviértela en tabla o amplía la tabla existente."), problemas);
    }

    @Test
    void reportaTodosLosProblemasJuntos() {
        XSSFWorkbook libro = new XSSFWorkbook();
        hojaCascada(libro, "GEPP", "Tabla3", new Object[]{"ALESTRA", "5511112222", null, "U", 0.0});
        fila(libro.createSheet("DENTALIA"), 5, 0, "LINEA", "CONSUMO(MB)");
        fila(libro.createSheet("METROBÚS"), 7, 0, "ICCID", "CONSUMO(MB)");
        XSSFSheet rara = libro.createSheet("RARA");
        fila(rara, 0, 0, "LINEA", "ICCID"); // sin columna de consumo
        fila(rara, 1, 0, "5511112222", null);
        tabla(rara, "Tabla9", 0, 0, 1, 1);
        byte[] archivo = contenido(libro);

        List<String> problemas = problemas(() -> lector.detectar(new ByteArrayInputStream(archivo)));

        assertEquals(3, problemas.size(), problemas.toString());
        assertTrue(problemas.get(0).startsWith("Hoja 'DENTALIA'"));
        assertTrue(problemas.get(1).startsWith("Hoja 'METROBÚS'"));
        assertTrue(problemas.get(2).startsWith("Hoja 'RARA', tabla 'Tabla9': La tabla tiene LINEA e ICCID pero no"
                + " CONSUMO(MB)"), problemas.get(2));
        PlantillaInvalidaException ex = assertThrows(PlantillaInvalidaException.class,
                () -> lector.detectar(new ByteArrayInputStream(archivo)));
        assertTrue(ex.getMessage().startsWith("La plantilla tiene 3 problemas:"), ex.getMessage());
    }

    @Test
    void iccidGuardadoComoNumeroEsUnProblemaConSusFilas() {
        XSSFWorkbook libro = new XSSFWorkbook();
        hojaCascada(libro, "DENTALIA", "Tabla17",
                new Object[]{"AT&T", null, 8.952070000000001E19, "U", 0.0},
                new Object[]{"AT&T", null, "89520700000000000011", "U", 0.0},
                new Object[]{"AT&T", null, 8.952070000000001E19, "U", 0.0});
        byte[] archivo = contenido(libro);

        List<String> problemas = problemas(() -> lector.detectar(new ByteArrayInputStream(archivo)));

        assertEquals(1, problemas.size());
        assertTrue(problemas.get(0).contains("ICCID guardado como número"), problemas.get(0));
        assertTrue(problemas.get(0).contains("en la fila 7, 9."), problemas.get(0));
    }

    @Test
    void unArchivoQueNoEsXlsxEsUnProblema() {
        InputStream texto = new ByteArrayInputStream("LINEA,CONSUMO\n".getBytes(StandardCharsets.UTF_8));

        List<String> problemas = problemas(() -> lector.detectar(texto));

        assertTrue(problemas.get(0).startsWith("El archivo no es un libro de Excel .xlsx válido"), problemas.get(0));
    }

    @Test
    void plantillaSinHojasDeClientesEsUnProblema() {
        XSSFWorkbook libro = new XSSFWorkbook();
        libro.createSheet("BD_ALESTRA");
        byte[] archivo = contenido(libro);

        assertEquals(List.of("La plantilla no tiene ninguna hoja de clientes con tabla nativa."),
                problemas(() -> lector.detectar(new ByteArrayInputStream(archivo))));
    }

    // ---------- Lectura con un mapeo guardado ----------

    @Test
    void leeConElMapeoGuardadoAunqueSeHayaCorregidoAMano() {
        // El usuario corrigió el mapeo: la columna de líneas se llama "Número" en el archivo.
        XSSFWorkbook libro = new XSSFWorkbook();
        XSSFSheet hoja = libro.createSheet("GEPP");
        fila(hoja, 0, 0, "Número", "CONSUMO(MB)");
        fila(hoja, 1, 0, "5511112222", 0.0);
        tabla(hoja, "Tabla3", 0, 0, 1, 1);
        MapeoPlantilla corregido = new MapeoPlantilla(List.of(new MapeoTabla("GEPP", "Tabla3", "A1:B2",
                TipoTablaPlantilla.SIMPLE_LINEA, Map.of("LINEA", "Número", "CONSUMO(MB)", "CONSUMO(MB)"), List.of())));

        PlantillaLeida leida = lector.leer(bytes(libro), corregido);

        assertEquals("5511112222", leida.tablas().get(0).tablaConFilas().filas().get(0).linea());
    }

    @Test
    void conMapeoGuardadoFallaSiLaTablaOLaColumnaYaNoExisten() {
        XSSFWorkbook libro = new XSSFWorkbook();
        hojaCascada(libro, "GEPP", "Tabla3", new Object[]{"ALESTRA", "5511112222", null, "U", 0.0});
        byte[] archivo = contenido(libro);
        MapeoPlantilla mapeo = new MapeoPlantilla(List.of(
                new MapeoTabla("GEPP", "Tabla3", "A6:E7", TipoTablaPlantilla.SIMPLE_LINEA,
                        Map.of("LINEA", "LÍNEA VIEJA", "CONSUMO(MB)", "CONSUMO(MB)"), List.of()),
                new MapeoTabla("GEPP", "TablaBorrada", "A20:B21", TipoTablaPlantilla.SIMPLE_LINEA,
                        Map.of("LINEA", "LINEA", "CONSUMO(MB)", "CONSUMO(MB)"), List.of()),
                new MapeoTabla("BORRADA", "Tabla1", "A1:B2", TipoTablaPlantilla.SIMPLE_LINEA,
                        Map.of("LINEA", "LINEA", "CONSUMO(MB)", "CONSUMO(MB)"), List.of())));

        List<String> problemas = problemas(() -> lector.leer(new ByteArrayInputStream(archivo), mapeo));

        assertEquals(List.of(
                "Hoja 'GEPP', tabla 'Tabla3': ya no tiene la columna 'LÍNEA VIEJA' (LINEA). Corrige el mapeo o"
                        + " vuelve a subir la plantilla.",
                "Tabla 'GEPP · TablaBorrada': ya no existe en el archivo.",
                "Tabla 'BORRADA · Tabla1': ya no existe en el archivo (no está la hoja)."), problemas);
    }

    // ---------- Corrección manual del mapeo ----------

    /** Tabla cuya columna de líneas se llama "Número": se detecta como SIMPLE_ICCID. */
    private static byte[] tablaConNumero() {
        XSSFWorkbook libro = new XSSFWorkbook();
        XSSFSheet hoja = libro.createSheet("GEPP");
        fila(hoja, 0, 0, "Número", "ICCID", "Notas", "CONSUMO(MB)");
        fila(hoja, 1, 0, "5511112222", "89520700000000000011", "x", 0.0);
        tabla(hoja, "Tabla3", 0, 0, 1, 3);
        return contenido(libro);
    }

    @Test
    void reclasificaUnaTablaConElMapeoCorregidoAMano() {
        byte[] archivo = tablaConNumero();
        assertEquals(TipoTablaPlantilla.SIMPLE_ICCID,
                lector.detectar(new ByteArrayInputStream(archivo)).tablas().get(0).tipo());

        MapeoTabla corregida = lector.reclasificar(new ByteArrayInputStream(archivo), "GEPP", "Tabla3",
                Map.of("LINEA", "Número", "ICCID", "ICCID", "CONSUMO(MB)", "CONSUMO(MB)"));

        assertEquals(new MapeoTabla("GEPP", "Tabla3", "A1:D2", TipoTablaPlantilla.SIMPLE_CASCADA,
                Map.of("LINEA", "Número", "ICCID", "ICCID", "CONSUMO(MB)", "CONSUMO(MB)"),
                List.of("Notas")), corregida);
    }

    @Test
    void laCorreccionRechazaCamposColumnasYTablasQueNoExisten() {
        byte[] archivo = tablaConNumero();

        List<String> problemas = problemas(() -> lector.reclasificar(new ByteArrayInputStream(archivo), "GEPP",
                "Tabla3", Map.of("TELEFONO", "Número", "ICCID", "No existe", "CONSUMO(MB)", "CONSUMO(MB)")));
        assertEquals(2, problemas.size(), problemas.toString());
        assertTrue(problemas.stream().anyMatch(p -> p.contains("'TELEFONO' no es un campo lógico")), problemas.toString());
        assertTrue(problemas.stream().anyMatch(p -> p.contains("no tiene la columna 'No existe' (para ICCID)")),
                problemas.toString());

        assertEquals(List.of("Hoja 'GEPP', tabla 'Otra': no existe en la plantilla activa."),
                problemas(() -> lector.reclasificar(new ByteArrayInputStream(archivo), "GEPP", "Otra", Map.of())));
    }

    @Test
    void laCorreccionRechazaUnaColumnaParaDosCamposOUnaTablaQueNoSePuedeClasificar() {
        byte[] archivo = tablaConNumero();

        List<String> dosCampos = problemas(() -> lector.reclasificar(new ByteArrayInputStream(archivo), "GEPP",
                "Tabla3", Map.of("LINEA", "Número", "ICCID", "Número")));
        assertTrue(dosCampos.get(0).contains("se asignó a dos campos"), dosCampos.toString());

        List<String> sinConsumo = problemas(() -> lector.reclasificar(new ByteArrayInputStream(archivo), "GEPP",
                "Tabla3", Map.of("LINEA", "Número")));
        assertTrue(sinConsumo.get(0).contains("no CONSUMO(MB)"), sinConsumo.toString());
    }
}
