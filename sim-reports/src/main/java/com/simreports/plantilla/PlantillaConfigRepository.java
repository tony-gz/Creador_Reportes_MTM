package com.simreports.plantilla;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlantillaConfigRepository extends JpaRepository<PlantillaConfig, Long> {

    /**
     * Lectura simple de la version activa, sin lock. Uso: GET
     * /api/plantilla/mapeo (spec 04) y cualquier lectura que no vaya a
     * escribir en la misma transaccion.
     */
    Optional<PlantillaConfig> findByActivaTrue();

    /**
     * Version con lock de fila (SELECT ... FOR UPDATE), para usar dentro
     * de un metodo @Transactional del servicio de plantilla al: (a) leer
     * la version activa para generar un reporte, o (b) reemplazarla por
     * una version nueva. Evita que dos requests simultaneos se pisen
     * (spec 04) sin necesidad de locks distribuidos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlantillaConfig p where p.activa = true")
    Optional<PlantillaConfig> findActivaParaActualizar();

    Optional<PlantillaConfig> findByVersion(int version);

    /** Listado de versiones historicas (GET /api/plantilla/versiones, spec 04), mas reciente primero. */
    List<PlantillaConfig> findAllByOrderByVersionDesc();

    /**
     * Siguiente numero de version a usar al insertar una fila nueva
     * (spec 04: autoincremental, nunca se reutiliza). Devuelve 0 si la
     * tabla esta vacia, para que el servicio lo use como base de su
     * calculo de "+1".
     */
    @Query("select coalesce(max(p.version), 0) from PlantillaConfig p")
    int obtenerMaxVersion();
}