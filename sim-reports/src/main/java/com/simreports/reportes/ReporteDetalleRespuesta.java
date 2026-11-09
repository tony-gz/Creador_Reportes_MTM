package com.simreports.reportes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/**
 * Detalle de una corrida ({@code POST /api/reportes} y {@code GET
 * /api/reportes/{id}}): el reporte final en texto, listo para mostrarlo, y lo
 * guardado como JSON (archivos usados, consumo sin cliente, avisos con
 * totales y SIMs repetidas) como objetos.
 */
public record ReporteDetalleRespuesta(
        Long id,
        Instant fecha,
        int periodoAnio,
        int periodoMes,
        String nombreArchivo,
        int versionPlantilla,
        String urlDescarga,
        String reporteFinal,
        JsonNode archivosEntrada,
        JsonNode consumoSinCliente,
        JsonNode avisos) {

    static ReporteDetalleRespuesta de(ReporteGenerado r, JsonMapper json) {
        return new ReporteDetalleRespuesta(r.getId(), r.getFecha(), r.getPeriodoAnio(), r.getPeriodoMes(),
                r.getNombreArchivo(), r.getPlantillaVersion().getVersion(), ReporteResumenRespuesta.urlDescarga(r.getId()),
                r.getReporteFinal(), json.readTree(r.getArchivosEntrada()), json.readTree(r.getLineasNoEncontradas()),
                json.readTree(r.getAvisos()));
    }
}
