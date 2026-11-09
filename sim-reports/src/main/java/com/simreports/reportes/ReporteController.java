package com.simreports.reportes;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Endpoints de reportes — spec 04 y Fase 2 punto 5 (spec 08). Errores (vía
 * {@code ManejadorErrores}): archivo de operador inválido o periodo inválido →
 * 400; sin plantilla activa → 409; reporte inexistente → 404; reporte escrito
 * que no cuadra → 500.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService servicio;
    private final JsonMapper json;

    public ReporteController(ReporteService servicio, JsonMapper json) {
        this.servicio = Objects.requireNonNull(servicio, "servicio no puede ser null");
        this.json = Objects.requireNonNull(json, "json no puede ser null");
    }

    /**
     * Genera el reporte de un periodo con los 3 archivos de los operadores
     * (campos {@code alestra}, {@code telcel}, {@code att}) y la plantilla activa.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReporteDetalleRespuesta> generar(
            @RequestParam("anio") int anio,
            @RequestParam("mes") int mes,
            @RequestParam("alestra") MultipartFile alestra,
            @RequestParam("telcel") MultipartFile telcel,
            @RequestParam("att") MultipartFile att) {
        Periodo periodo;
        try {
            periodo = new Periodo(anio, mes);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        ReporteResultado r = servicio.generar(periodo, entrada("Alestra", alestra), entrada("Telcel", telcel),
                entrada("ATT", att));
        return ResponseEntity.created(URI.create("/api/reportes/" + r.reporte().getId()))
                .body(ReporteDetalleRespuesta.de(r.reporte(), json));
    }

    /** Historial de corridas, la más reciente primero. */
    @GetMapping
    public List<ReporteResumenRespuesta> listar() {
        return servicio.listar().stream().map(ReporteResumenRespuesta::de).toList();
    }

    /** Detalle de una corrida, con su reporte final. */
    @GetMapping("/{id}")
    public ReporteDetalleRespuesta detalle(@PathVariable Long id) {
        return ReporteDetalleRespuesta.de(servicio.obtener(id), json);
    }

    /** Descarga el .xlsx generado, con su nombre (p. ej. CONSUMOS_OCTUBRE_2026.xlsx). */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        ArchivoDescarga archivo = servicio.descargar(id);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.contenido());
    }

    private static ArchivoEntrada entrada(String operador, MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo de " + operador + " está vacío.");
        }
        try {
            return new ArchivoEntrada(archivo.getOriginalFilename(), archivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo de " + operador, e);
        }
    }
}
