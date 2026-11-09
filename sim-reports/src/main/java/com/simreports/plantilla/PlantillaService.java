package com.simreports.plantilla;

import com.simreports.almacenamiento.AlmacenamientoArchivos;
import com.simreports.core.plantilla.LectorPlantilla;
import com.simreports.core.plantilla.MapeoPlantilla;
import com.simreports.core.plantilla.PlantillaInvalidaException;
import com.simreports.core.plantilla.PlantillaLeida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
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
 *   <li>{@link #mapeoActivo}: el mapeo de la versión activa.</li>
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
    public PlantillaConfig registrarNuevaVersion(byte[] contenido) {
        Objects.requireNonNull(contenido, "contenido no puede ser null");
        MapeoPlantilla mapeo = lector.detectar(new ByteArrayInputStream(contenido));

        Optional<PlantillaConfig> anterior = repositorio.findActivaParaActualizar();
        int version = repositorio.obtenerMaxVersion() + 1;
        String ruta = almacenamiento.guardarPlantilla(version, contenido);

        anterior.ifPresent(a -> a.setActiva(false));
        return repositorio.save(new PlantillaConfig(version, ruta, mapeoJson.escribir(mapeo), true));
    }

    /** Mapeo de la versión activa, o vacío si todavía no hay plantilla. */
    @Transactional(readOnly = true)
    public Optional<MapeoPlantilla> mapeoActivo() {
        return repositorio.findByActivaTrue().map(p -> mapeoJson.leer(p.getMapeoColumnas()));
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
