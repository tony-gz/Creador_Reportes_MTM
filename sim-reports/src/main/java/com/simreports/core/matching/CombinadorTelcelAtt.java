package com.simreports.core.matching;

import com.simreports.core.AvisoParseo;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Combina los datasets ya parseados de Telcel y ATT en un solo
 * {@link ResultadoParseo} indexado por ICCID — spec 03, regla de
 * cascada: "ICCID contra Telcel + ATT combinados".
 *
 * <p>Decisión no cubierta explícitamente por las specs 02/03: si el
 * mismo ICCID aparece en ambos archivos (por ejemplo, un equipo que migra
 * de operador a mitad de mes), se aplica la misma política de "nunca
 * perder MB reales" que ya usan los parsers individuales (spec 02,
 * Alestra/Telcel: sumar en vez de quedarse con uno) y se registra como
 * {@link TipoAviso#CLAVE_DUPLICADA} — mismo tipo de aviso ya definido,
 * porque semánticamente es el mismo caso: la clave llegó más de una vez
 * y hay que decidir qué hacer con los MB de cada aparición.</p>
 *
 * <p>Puro: no conoce Excel ni Spring, solo combina dos {@code Map} en
 * memoria. Se prueba con JUnit puro usando fixtures.</p>
 */
public final class CombinadorTelcelAtt {

    private CombinadorTelcelAtt() {
    }

    public static ResultadoParseo combinar(ResultadoParseo telcel, ResultadoParseo att) {
        Objects.requireNonNull(telcel, "telcel no puede ser null");
        Objects.requireNonNull(att, "att no puede ser null");

        Map<String, Double> combinado = new HashMap<>(telcel.consumos());
        List<AvisoParseo> avisos = new ArrayList<>(telcel.avisos());
        avisos.addAll(att.avisos());

        for (Map.Entry<String, Double> entradaAtt : att.consumos().entrySet()) {
            String iccid = entradaAtt.getKey();
            Double consumoAtt = entradaAtt.getValue();
            Double consumoTelcelPrevio = combinado.get(iccid);

            if (consumoTelcelPrevio == null) {
                combinado.put(iccid, consumoAtt);
                continue;
            }

            double suma = consumoTelcelPrevio + consumoAtt;
            combinado.put(iccid, suma);
            avisos.add(new AvisoParseo(
                    TipoAviso.CLAVE_DUPLICADA,
                    iccid,
                    "ICCID presente tanto en Telcel (" + consumoTelcelPrevio + " MB) como en ATT ("
                            + consumoAtt + " MB); se sumaron ambos consumos al combinar las dos fuentes."));
        }

        return new ResultadoParseo(combinado, avisos);
    }
}