package com.simreports.reportes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Long> {

    /** GET /api/reportes (spec 04): historial de corridas, mas reciente primero. */
    List<ReporteGenerado> findAllByOrderByFechaDesc();
}