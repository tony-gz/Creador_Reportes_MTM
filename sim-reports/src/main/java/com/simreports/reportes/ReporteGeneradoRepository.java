package com.simreports.reportes;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Long> {

    /**
     * GET /api/reportes (spec 04): historial de corridas, mas reciente primero.
     * Trae la version de plantilla en la misma consulta (open-in-view esta
     * apagado, asi que la relacion LAZY no se podria leer despues).
     */
    @EntityGraph(attributePaths = "plantillaVersion")
    List<ReporteGenerado> findAllByOrderByFechaDescIdDesc();

    /** Una corrida con su version de plantilla (GET /api/reportes/{id}). */
    @EntityGraph(attributePaths = "plantillaVersion")
    Optional<ReporteGenerado> findConVersionById(Long id);
}