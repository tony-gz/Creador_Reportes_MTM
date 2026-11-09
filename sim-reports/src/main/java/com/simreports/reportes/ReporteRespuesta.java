package com.simreports.reportes;

import com.simreports.core.matching.NoEncontrado;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Respuesta de {@code POST /api/reportes}: el detalle de la corrida y el
 * .xlsx en Base64, para que el navegador lo descargue sin que el servidor lo
 * guarde (D-023).
 */
public record ReporteRespuesta(
        Instant fecha,
        int periodoAnio,
        int periodoMes,
        String nombreArchivo,
        String reporteFinal,
        List<ReporteResultado.ArchivoUsado> archivosEntrada,
        List<NoEncontrado> consumoSinCliente,
        Map<String, Object> avisos,
        String xlsxBase64) {

    static ReporteRespuesta de(ReporteResultado r) {
        Map<String, Object> avisos = new LinkedHashMap<>();
        avisos.put("totales", r.resultado().totales());
        avisos.put("advertenciasConsistencia", r.resultado().advertenciasConsistencia());
        avisos.put("simsRepetidas", r.resultado().simsRepetidas());
        avisos.put("avisosParseo", r.resultado().avisosParseo());
        avisos.put("formulasSinRecalcular", r.formulasSinRecalcular());
        return new ReporteRespuesta(Instant.now(), r.periodo().anio(), r.periodo().mes(), r.nombreArchivo(),
                r.reporteFinal(), r.archivosEntrada(), r.resultado().noEncontrados(), avisos,
                Base64.getEncoder().encodeToString(r.contenido()));
    }
}
