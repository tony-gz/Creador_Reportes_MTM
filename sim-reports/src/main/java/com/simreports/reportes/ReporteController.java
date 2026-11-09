package com.simreports.reportes;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Generar un reporte — D-023 (spec 12): recibe la plantilla y los 3 archivos,
 * devuelve el detalle con el .xlsx y no guarda nada. Errores (vía
 * {@code ManejadorErrores}): plantilla, archivo o periodo inválido → 400;
 * reporte escrito que no cuadra → 500.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = Objects.requireNonNull(servicio, "servicio no puede ser null");
    }

    /** Campos {@code anio}, {@code mes}, {@code plantilla}, {@code alestra}, {@code telcel} y {@code att}. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReporteRespuesta generar(
            @RequestParam("anio") int anio,
            @RequestParam("mes") int mes,
            @RequestParam("plantilla") MultipartFile plantilla,
            @RequestParam("alestra") MultipartFile alestra,
            @RequestParam("telcel") MultipartFile telcel,
            @RequestParam("att") MultipartFile att) {
        Periodo periodo;
        try {
            periodo = new Periodo(anio, mes);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return ReporteRespuesta.de(servicio.generar(periodo, entrada("la plantilla", plantilla),
                entrada("Alestra", alestra), entrada("Telcel", telcel), entrada("ATT", att)));
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
