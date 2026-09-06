package com.simreports.core.matching;

import com.simreports.core.AvisoParseo;

import java.util.List;
import java.util.Objects;

/**
 * Reportes finales del motor de coincidencia, calculados una vez que se
 * resolvieron **todas** las tablas de la plantilla — ver
 * {@link MotorCoincidencia#finalizar()} y spec 03.
 *
 * <p>Los valores a escribir por celda/tabla (spec 03, "Contrato de
 * entrada del motor") no viven en este record: se obtienen antes, tabla
 * por tabla, con {@link MotorCoincidencia#resolverTabla} /
 * {@link MotorCoincidencia#resolverTablas}. Este record solo agrupa lo
 * que únicamente puede calcularse después de agotar el matching contra
 * las 18 hojas: qué quedó sin consultar y si los totales cuadran.</p>
 *
 * @param avisosParseo avisos heredados de los parsers de origen (spec 02:
 *                      duplicados agregados, claves vacías/malformadas),
 *                      para que la UI los muestre junto con los propios
 *                      del motor.
 */
public record ResultadoMatching(
        List<NoEncontrado> noEncontrados,
        List<AdvertenciaConsistencia> advertenciasConsistencia,
        List<AvisoParseo> avisosParseo) {

    public ResultadoMatching {
        Objects.requireNonNull(noEncontrados, "noEncontrados no puede ser null");
        Objects.requireNonNull(advertenciasConsistencia, "advertenciasConsistencia no puede ser null");
        Objects.requireNonNull(avisosParseo, "avisosParseo no puede ser null");
        noEncontrados = List.copyOf(noEncontrados);
        advertenciasConsistencia = List.copyOf(advertenciasConsistencia);
        avisosParseo = List.copyOf(avisosParseo);
    }
}