package com.simreports.plantilla;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * Una version inmutable de la plantilla de reporte (spec 04).
 *
 * Cada vez que el usuario sube un nuevo archivo de plantilla se inserta
 * una fila nueva (nunca se hace UPDATE sobre rutaArchivo/version de una
 * fila existente) y se marca activa=true en la nueva, activa=false en la
 * anterior. Esto permite reabrir cualquier reporte historico sabiendo
 * exactamente que mapeo de columnas se uso para generarlo.
 *
 * La invariante "solo una fila activa=true a la vez" NO se enforce con un
 * constraint de base de datos (partial unique index): se resuelve a nivel
 * de aplicacion con un lock de fila (SELECT ... FOR UPDATE, ver
 * PlantillaConfigRepository.findActivaParaActualizar) dentro de un metodo
 * @Transactional del servicio de plantilla (spec 04, punto 3 de Fase 2) --
 * es la misma decision de "un solo usuario, sin infraestructura de locks
 * distribuidos" ya tomada en esa spec.
 */
@Entity
@Table(
        name = "plantilla_config",
        uniqueConstraints = @UniqueConstraint(name = "ux_plantilla_config_version", columnNames = "version")
)
public class PlantillaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Autoincremental, nunca se reutiliza. La unicidad la garantiza el
     * constraint de la tabla; el calculo del "siguiente numero de
     * version" (max + 1) vive en el servicio de plantilla, no aqui.
     */
    @Column(nullable = false, updatable = false)
    private int version;

    /** Ruta en disco del .xlsx de esta version (nunca se guarda el binario en Postgres, ver spec 04). */
    @Column(name = "ruta_archivo", nullable = false, updatable = false)
    private String rutaArchivo;

    /**
     * JSON con el resultado persistido del resolver (spec 01) para esta
     * version: por tabla/hoja, el TipoTablaPlantilla, el mapeo de campo
     * logico -> encabezado real, y los encabezados no reconocidos.
     * Se guarda como texto plano (no jsonb nativo): no se hacen consultas
     * dentro del JSON, solo se lee/escribe entero, asi que un CLOB/TEXT
     * portable entre Postgres y H2 (tests) es suficiente y mas simple que
     * mapear un tipo jsonb especifico.
     */
    @Lob
    @Column(name = "mapeo_columnas", nullable = false)
    private String mapeoColumnas;

    /** Solo una fila tiene activa=true a la vez (ver nota de clase). */
    @Column(nullable = false)
    private boolean activa;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected PlantillaConfig() {
        // Requerido por JPA/Hibernate.
    }

    public PlantillaConfig(int version, String rutaArchivo, String mapeoColumnas, boolean activa) {
        this.version = version;
        this.rutaArchivo = Objects.requireNonNull(rutaArchivo, "rutaArchivo no puede ser null");
        this.mapeoColumnas = Objects.requireNonNull(mapeoColumnas, "mapeoColumnas no puede ser null");
        this.activa = activa;
    }

    public Long getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public String getMapeoColumnas() {
        return mapeoColumnas;
    }

    /**
     * PUT /api/plantilla/mapeo (spec 04) corrige el mapeo de la version
     * activa sin crear una version nueva, porque no cambia el archivo,
     * solo la interpretacion de sus columnas.
     */
    public void setMapeoColumnas(String mapeoColumnas) {
        this.mapeoColumnas = Objects.requireNonNull(mapeoColumnas, "mapeoColumnas no puede ser null");
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlantillaConfig other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}