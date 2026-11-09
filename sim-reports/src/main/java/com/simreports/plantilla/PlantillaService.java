package com.simreports.plantilla;

import com.simreports.almacenamiento.AlmacenamientoArchivos;
import com.simreports.core.plantilla.LectorPlantilla;
import com.simreports.core.plantilla.MapeoPlantilla;
import com.simreports.core.plantilla.MapeoTabla;
import com.simreports.core.plantilla.PlantillaInvalidaException;
import com.simreports.core.plantilla.PlantillaLeida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de plantilla — Fase 2 punto 2 (spec 08) y spec 04.
 *
 * <ul>
 *   <li>{@link #registrarNuevaVersion}: valida el archivo con
 *       {@link LectorPlantilla#detectar}, lo guarda en disco y crea una
 *       versión nueva activa, desactivando la anterior, en una sola
 *       transacción con lock sobre la fila activa.</li>
 *   <li>{@link #activa}: la versión activa con su mapeo.</li>
 *   <li>{@link #corregirMapeo}: corrige a mano el mapeo de una tabla de la
 *       versión activa, sin crear una versión nueva (spec 04).</li>
 *   <li>{@link #versiones}: historial de versiones.</li>
 *   <li>{@link #leerActivaParaReporte}: la versión activa con sus filas leídas
 *       con el mapeo guardado, para generar un reporte.</li>
 * </ul>
 */
@Service
public class PlantillaService {

    private final PlantillaConfigRepository repositorio;
    private final AlmacenamientoArchivos almacenamiento;
    private final MapeoPlantillaJson mapeoJson;
    private final LectorPlantilla lector = new LectorPlantilla();

    public PlantillaService(
            PlantillaConfigRepository repositorio,
            AlmacenamientoArchivos almacenamiento,
            MapeoPlantillaJson mapeoJson) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio no puede ser null");
        this.almacenamiento = Objects.requireNonNull(almacenamiento, "almacenamiento no puede ser null");
        this.mapeoJson = Objects.requireNonNull(mapeoJson, "mapeoJson no puede ser null");
    }

    /**
     * Sube una plantilla nueva. Si el archivo tiene problemas no se guarda nada.
     *
     * <p>Orden: (1) validar, antes de tocar disco o base; (2) lock sobre la
     * versión activa ({@code SELECT ... FOR UPDATE}) para serializar dos
     * subidas simultáneas; (3) siguiente número de versión; (4) guardar el
     * archivo; (5) insertar la nueva activa y desactivar la anterior. La
     * restricción diferida de la base comprueba "una sola activa" al commit.</p>
     *
     * @throws PlantillaInvalidaException con todos los problemas del archivo.
     */
    @Transactional
    public PlantillaActiva registrarNuevaVersion(byte[] contenido) {
        Objects.requireNonNull(contenido, "contenido no puede ser null");
        MapeoPlantilla mapeo = lector.detectar(new ByteArrayInputStream(contenido));

        Optional<PlantillaConfig> anterior = repositorio.findActivaParaActualizar();
        int version = repositorio.obtenerMaxVersion() + 1;
        String ruta = almacenamiento.guardarPlantilla(version, contenido);

        anterior.ifPresent(a -> a.setActiva(false));
        PlantillaConfig nueva = repositorio.save(new PlantillaConfig(version, ruta, mapeoJson.escribir(mapeo), true));
        return new PlantillaActiva(nueva, mapeo);
    }

    /** Versión activa con su mapeo, o vacío si todavía no hay plantilla. */
    @Transactional(readOnly = true)
    public Optional<PlantillaActiva> activa() {
        return repositorio.findByActivaTrue().map(p -> new PlantillaActiva(p, mapeoJson.leer(p.getMapeoColumnas())));
    }

    /** Todas las versiones, la más reciente primero. */
    @Transactional(readOnly = true)
    public List<PlantillaConfig> versiones() {
        return repositorio.findAllByOrderByVersionDesc();
    }

    /**
     * Corrige a mano el mapeo de una tabla de la versión activa (spec 04): no
     * crea una versión nueva, porque el archivo no cambia, solo cómo se leen
     * sus columnas. La tabla se reclasifica con el mapeo nuevo y después se
     * comprueba que toda la plantilla se pueda leer con él.
     *
     * @param columnaPorCampoLogico campo lógico → encabezado real; reemplaza
     *        completo el mapeo de esa tabla.
     * @throws SinPlantillaActivaException si todavía no hay plantilla.
     * @throws PlantillaInvalidaException  si la tabla no está en el mapeo o la
     *         corrección no es válida; el mapeo guardado no cambia.
     */
    @Transactional
    public PlantillaActiva corregirMapeo(String hoja, String nombreTabla, Map<String, String> columnaPorCampoLogico) {
        PlantillaConfig activa = repositorio.findActivaParaActualizar().orElseThrow(SinPlantillaActivaException::new);
        MapeoPlantilla mapeo = mapeoJson.leer(activa.getMapeoColumnas());
        int posicion = -1;
        for (int i = 0; i < mapeo.tablas().size(); i++) {
            MapeoTabla t = mapeo.tablas().get(i);
            if (t.hoja().equals(hoja) && t.nombreTabla().equals(nombreTabla)) {
                posicion = i;
            }
        }
        if (posicion < 0) {
            throw new PlantillaInvalidaException(List.of("Hoja '" + hoja + "', tabla '" + nombreTabla
                    + "': no está en el mapeo de la plantilla activa (versión " + activa.getVersion() + ")."));
        }
        byte[] contenido = almacenamiento.leer(activa.getRutaArchivo());
        MapeoTabla corregida = lector.reclasificar(new ByteArrayInputStream(contenido), hoja, nombreTabla,
                columnaPorCampoLogico);
        List<MapeoTabla> tablas = new ArrayList<>(mapeo.tablas());
        tablas.set(posicion, corregida);
        MapeoPlantilla nuevo = new MapeoPlantilla(tablas);
        lector.leer(new ByteArrayInputStream(contenido), nuevo); // falla si con el mapeo nuevo no se puede leer
        activa.setMapeoColumnas(mapeoJson.escribir(nuevo));
        return new PlantillaActiva(activa, nuevo);
    }

    /**
     * Versión activa con sus filas leídas con el mapeo guardado. Toma el lock
     * de la fila activa: si quien llama tiene su propia transacción (el
     * servicio de reportes), el lock dura toda la generación y un reemplazo
     * simultáneo de la plantilla espera.
     *
     * @throws SinPlantillaActivaException si todavía no hay plantilla.
     * @throws PlantillaInvalidaException  si el archivo ya no coincide con el
     *         mapeo guardado (p. ej. se corrigió mal a mano).
     */
    @Transactional
    public PlantillaParaReporte leerActivaParaReporte() {
        PlantillaConfig activa = repositorio.findActivaParaActualizar().orElseThrow(SinPlantillaActivaException::new);
        byte[] contenido = almacenamiento.leer(activa.getRutaArchivo());
        MapeoPlantilla mapeo = mapeoJson.leer(activa.getMapeoColumnas());
        PlantillaLeida leida = lector.leer(new ByteArrayInputStream(contenido), mapeo);
        return new PlantillaParaReporte(activa, contenido, leida);
    }
}
