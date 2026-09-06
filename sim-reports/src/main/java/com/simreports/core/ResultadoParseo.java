package com.simreports.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Contrato de salida común de los tres parsers de origen (Alestra,
 * Telcel, ATT) — ver spec 02, sección "Interfaz común".
 *
 * @param consumos mapa ya agregado: clave (línea o ICCID, según el
 *                 operador) → consumo en MB. Sin duplicados: si la fuente
 *                 traía la misma clave varias veces, ya viene sumada aquí
 *                 (ver {@link TipoAviso#CLAVE_DUPLICADA}).
 * @param avisos   avisos no bloqueantes recogidos durante el parseo
 *                 (duplicados agregados, claves vacías o malformadas
 *                 excluidas). Nunca {@code null}; lista vacía si no hubo
 *                 avisos.
 */
public record ResultadoParseo(Map<String, Double> consumos, List<AvisoParseo> avisos) {

    public ResultadoParseo {
        Objects.requireNonNull(consumos, "consumos no puede ser null");
        Objects.requireNonNull(avisos, "avisos no puede ser null (usar List.of() si no hay avisos)");
        // Copias inmutables: el resultado de un parseo no debe mutarse
        // después de construido (los consumidores —motor de coincidencia,
        // capa web— solo deben leerlo).
        consumos = Map.copyOf(consumos);
        avisos = List.copyOf(avisos);
    }
}
