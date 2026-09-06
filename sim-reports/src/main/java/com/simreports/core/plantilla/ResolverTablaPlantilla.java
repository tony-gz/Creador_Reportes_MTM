package com.simreports.core.plantilla;

import com.simreports.core.HeaderResolver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Clasifica una tabla de la plantilla a partir de sus encabezados reales
 * — ver spec 01, sección 3 ("Resolver de tablas de la plantilla") y
 * spec 03 ("Reglas por tipo de tabla").
 *
 * <p>Puro: no conoce Excel ni Apache POI. Recibe la lista de encabezados
 * reales de una tabla ya detectada (por ejemplo, vía
 * {@code XSSFTable.getColumns()} en la capa que sí usa POI) y decide su
 * tipo. Esto permite probar toda la lógica de clasificación con fixtures
 * en memoria, sin tocar archivos Excel reales.</p>
 */
public class ResolverTablaPlantilla {

    // Campos lógicos reconocidos por PlantillaHeaderResolver (spec 01).
    private static final String LINEA = "LINEA";
    private static final String LINEA_1 = "LINEA_1";
    private static final String LINEA_2 = "LINEA_2";
    private static final String ICCID = "ICCID";
    private static final String CONSUMO = "CONSUMO(MB)";
    private static final String CONSUMO_1 = "CONSUMO(MB)_1";
    private static final String CONSUMO_2 = "CONSUMO(MB)_2";

    private final HeaderResolver headerResolver;

    public ResolverTablaPlantilla() {
        this(new PlantillaHeaderResolver());
    }

    public ResolverTablaPlantilla(HeaderResolver headerResolver) {
        this.headerResolver = Objects.requireNonNull(headerResolver, "headerResolver no puede ser null");
    }

    /**
     * Resuelve y clasifica una tabla a partir de sus encabezados reales,
     * en el orden en que aparecen en la tabla de Excel.
     *
     * @throws TablaPlantillaNoReconocidaException si dos encabezados
     *         distintos resuelven al mismo campo lógico, o si los campos
     *         lógicos encontrados no alcanzan para clasificar la tabla en
     *         ninguno de los cuatro tipos conocidos (por ejemplo, falta
     *         la columna de consumo donde escribir el resultado, o una
     *         tabla dual SIM incompleta).
     */
    public TablaPlantillaResuelta resolver(List<String> headersReales) {
        Objects.requireNonNull(headersReales, "headersReales no puede ser null");

        Map<String, String> columnaPorCampoLogico = new LinkedHashMap<>();
        List<String> noReconocidos = new ArrayList<>();

        for (String headerReal : headersReales) {
            Optional<String> campoLogico = headerResolver.resolver(headerReal);
            if (campoLogico.isEmpty()) {
                noReconocidos.add(headerReal);
                continue;
            }
            String campo = campoLogico.get();
            String anterior = columnaPorCampoLogico.putIfAbsent(campo, headerReal);
            if (anterior != null) {
                throw new TablaPlantillaNoReconocidaException(
                        "El campo lógico '" + campo + "' quedó resuelto por dos columnas distintas en la "
                                + "misma tabla: '" + anterior + "' y '" + headerReal + "'. Revisa los "
                                + "encabezados de la hoja.");
            }
        }

        TipoTablaPlantilla tipo = clasificar(columnaPorCampoLogico.keySet(), headersReales);
        return new TablaPlantillaResuelta(tipo, columnaPorCampoLogico, noReconocidos);
    }

    private TipoTablaPlantilla clasificar(Set<String> campos, List<String> headersRealesOriginales) {
        boolean tieneLinea = campos.contains(LINEA);
        boolean tieneLinea1 = campos.contains(LINEA_1);
        boolean tieneLinea2 = campos.contains(LINEA_2);
        boolean tieneIccid = campos.contains(ICCID);
        boolean tieneConsumo = campos.contains(CONSUMO);
        boolean tieneConsumo1 = campos.contains(CONSUMO_1);
        boolean tieneConsumo2 = campos.contains(CONSUMO_2);

        // Cualquier señal de tabla dual SIM (spec 03, regla 4) exige las
        // cuatro columnas completas; una señal parcial es un error de la
        // plantilla, no un caso a adivinar.
        if (tieneLinea1 || tieneLinea2 || tieneConsumo1 || tieneConsumo2) {
            if (tieneLinea1 && tieneLinea2 && tieneConsumo1 && tieneConsumo2) {
                return TipoTablaPlantilla.DUAL_SIM;
            }
            throw new TablaPlantillaNoReconocidaException(
                    "La tabla parece ser dual SIM (router) pero le faltan columnas: se esperaban LINEA_1, "
                            + "LINEA_2, CONSUMO(MB)_1 y CONSUMO(MB)_2 juntas. Campos reconocidos: " + campos
                            + ". Encabezados reales de la tabla: " + headersRealesOriginales);
        }

        if (tieneLinea && tieneIccid) {
            if (!tieneConsumo) {
                throw new TablaPlantillaNoReconocidaException(
                        "La tabla tiene LINEA e ICCID pero no CONSUMO(MB); no hay columna donde escribir el "
                                + "consumo encontrado. Encabezados reales de la tabla: " + headersRealesOriginales);
            }
            return TipoTablaPlantilla.SIMPLE_CASCADA;
        }

        if (tieneLinea) {
            if (!tieneConsumo) {
                throw new TablaPlantillaNoReconocidaException(
                        "La tabla tiene LINEA pero no CONSUMO(MB); no hay columna donde escribir el consumo "
                                + "encontrado. Encabezados reales de la tabla: " + headersRealesOriginales);
            }
            return TipoTablaPlantilla.SIMPLE_LINEA;
        }

        if (tieneIccid) {
            if (!tieneConsumo) {
                throw new TablaPlantillaNoReconocidaException(
                        "La tabla tiene ICCID pero no CONSUMO(MB); no hay columna donde escribir el consumo "
                                + "encontrado. Encabezados reales de la tabla: " + headersRealesOriginales);
            }
            return TipoTablaPlantilla.SIMPLE_ICCID;
        }

        throw new TablaPlantillaNoReconocidaException(
                "No se pudo clasificar la tabla: no se reconoció ninguna columna de línea ni de ICCID. "
                        + "Encabezados reales de la tabla: " + headersRealesOriginales);
    }
}
