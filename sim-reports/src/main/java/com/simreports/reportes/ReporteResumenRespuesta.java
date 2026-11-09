package com.simreports.reportes;

import java.time.Instant;

/** Una corrida en el historial ({@code GET /api/reportes}). */
public record ReporteResumenRespuesta(
        Long id,
        Instant fecha,
        int periodoAnio,
        int periodoMes,
        String nombreArchivo,
        int versionPlantilla,
        String urlDescarga) {

    static ReporteResumenRespuesta de(ReporteGenerado r) {
        return new ReporteResumenRespuesta(r.getId(), r.getFecha(), r.getPeriodoAnio(), r.getPeriodoMes(),
                r.getNombreArchivo(), r.getPlantillaVersion().getVersion(), urlDescarga(r.getId()));
    }

    static String urlDescarga(Long id) {
        return "/api/reportes/" + id + "/descargar";
    }
}
