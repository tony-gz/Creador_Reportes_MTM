package com.simreports.web;

import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.plantilla.PlantillaInvalidaException;
import com.simreports.core.plantilla.TablaPlantillaNoReconocidaException;
import com.simreports.plantilla.SinPlantillaActivaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Traduce las excepciones de dominio a respuestas HTTP — spec 04 y Fase 2
 * punto 6 (spec 08). Formato estándar RFC 9457 ({@code application/problem+json}):
 * {@code title}, {@code status}, {@code detail} y, cuando aplica, la lista
 * {@code problemas} para mostrarla tal cual en la UI. Los errores propios de
 * Spring MVC (falta un parámetro, cuerpo ilegible, archivo demasiado grande...)
 * los resuelve {@link ResponseEntityExceptionHandler} con el mismo formato.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    @ExceptionHandler(PlantillaInvalidaException.class)
    public ProblemDetail plantillaInvalida(PlantillaInvalidaException e) {
        return problema(HttpStatus.BAD_REQUEST, "Plantilla inválida", e.getMessage(), e.getProblemas());
    }

    @ExceptionHandler(TablaPlantillaNoReconocidaException.class)
    public ProblemDetail tablaNoReconocida(TablaPlantillaNoReconocidaException e) {
        return problema(HttpStatus.BAD_REQUEST, "Tabla de la plantilla no reconocida", e.getMessage(),
                List.of(e.getMessage()));
    }

    @ExceptionHandler(ArchivoFormatoInvalidoException.class)
    public ProblemDetail archivoInvalido(ArchivoFormatoInvalidoException e) {
        return problema(HttpStatus.BAD_REQUEST, "Archivo con formato inválido", e.getMessage(),
                List.of(e.getMessage()));
    }

    @ExceptionHandler(SinPlantillaActivaException.class)
    public ProblemDetail sinPlantillaActiva(SinPlantillaActivaException e) {
        return problema(HttpStatus.CONFLICT, "Sin plantilla activa", e.getMessage(), List.of(e.getMessage()));
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle, List<String> problemas) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setProperty("problemas", problemas);
        return problema;
    }
}
