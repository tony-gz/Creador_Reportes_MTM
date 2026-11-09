package com.simreports.reportes;

import com.simreports.plantilla.PlantillaConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * Una corrida del motor de coincidencia (spec 03/04): que version de
 * plantilla se uso, que archivos de origen se subieron, donde quedo el
 * .xlsx resultado, y los tres tipos de hallazgos que ya vienen del motor
 * (no encontrados, avisos de parseo, advertencias de consistencia).
 *
 * archivosEntrada, lineasNoEncontradas y avisos se guardan como JSON en
 * columnas text (mismo criterio que PlantillaConfig.mapeoColumnas; sin
 * @Lob, que en Postgres seria oid): no se
 * consulta dentro del JSON, solo se lee/escribe entero para mostrarlo en
 * la UI (Fase 3) o para reabrir el detalle de una corrida pasada.
 */
@Entity
@Table(name = "reporte_generado")
public class ReporteGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant fecha;

    /**
     * Que version de plantilla se uso en esta corrida (spec 04: nuevo,
     * para reproducibilidad). Sin cascada de borrado configurada aqui a
     * proposito: spec 04 todavia no define una politica de retencion.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plantilla_version_id", nullable = false, updatable = false)
    private PlantillaConfig plantillaVersion;

    /**
     * JSON: que archivo (nombre original subido) se uso para cada
     * operador en esta corrida, p. ej.
     * {"alestra": "reporte_agosto.xlsx", "telcel": "export.csv", "att": "att_consolidado.xlsx"}.
     * La app nunca depende del nombre de archivo para el parsing (spec
     * 02) -- esto es solo trazabilidad para la UI/historial.
     */
    @Column(name = "archivos_entrada", nullable = false, columnDefinition = "text")
    private String archivosEntrada;

    /** Mes y año que reporta (Fase 2 punto 4); dan el nombre del archivo. */
    @Column(name = "periodo_anio", nullable = false, updatable = false)
    private int periodoAnio;

    @Column(name = "periodo_mes", nullable = false, updatable = false)
    private int periodoMes;

    /** Nombre del .xlsx generado, p. ej. CONSUMOS_OCTUBRE_2026.xlsx. */
    @Column(name = "nombre_archivo", nullable = false, updatable = false, length = 255)
    private String nombreArchivo;

    /**
     * Texto del reporte final de la corrida (ReporteFinalCorrida: totales,
     * SIMs repetidas, consumo sin cliente, avisos). Se muestra en la app; no
     * va dentro del .xlsx (decision del 2026-11-09).
     */
    @Column(name = "reporte_final", nullable = false, updatable = false, columnDefinition = "text")
    private String reporteFinal;

    /** Ruta en disco del .xlsx generado (relativa a la carpeta de almacenamiento), para re-descargarlo. */
    @Column(name = "ruta_archivo_resultado", nullable = false, updatable = false, length = 1024)
    private String rutaArchivoResultado;

    /** JSON de la lista de NoEncontrado del motor (spec 03). */
    @Column(name = "lineas_no_encontradas", nullable = false, columnDefinition = "text")
    private String lineasNoEncontradas;

    /**
     * JSON de avisos combinados: AvisoParseo (duplicados, claves
     * malformadas, heredados de spec 02) + AdvertenciaConsistencia (spec
     * 03), para que la UI los muestre todos juntos en un solo lugar.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String avisos;

    protected ReporteGenerado() {
        // Requerido por JPA/Hibernate.
    }

    public ReporteGenerado(
            PlantillaConfig plantillaVersion,
            int periodoAnio,
            int periodoMes,
            String nombreArchivo,
            String archivosEntrada,
            String rutaArchivoResultado,
            String lineasNoEncontradas,
            String avisos,
            String reporteFinal
    ) {
        this.plantillaVersion = Objects.requireNonNull(plantillaVersion, "plantillaVersion no puede ser null");
        this.periodoAnio = periodoAnio;
        this.periodoMes = periodoMes;
        this.nombreArchivo = Objects.requireNonNull(nombreArchivo, "nombreArchivo no puede ser null");
        this.reporteFinal = Objects.requireNonNull(reporteFinal, "reporteFinal no puede ser null");
        this.archivosEntrada = Objects.requireNonNull(archivosEntrada, "archivosEntrada no puede ser null");
        this.rutaArchivoResultado = Objects.requireNonNull(rutaArchivoResultado, "rutaArchivoResultado no puede ser null");
        this.lineasNoEncontradas = Objects.requireNonNull(lineasNoEncontradas, "lineasNoEncontradas no puede ser null");
        this.avisos = Objects.requireNonNull(avisos, "avisos no puede ser null");
    }

    public Long getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public PlantillaConfig getPlantillaVersion() {
        return plantillaVersion;
    }

    /** Atajo usado por la capa web/serializacion en vez de navegar la relacion completa. */
    public Long getPlantillaVersionId() {
        return plantillaVersion.getId();
    }

    public int getPeriodoAnio() {
        return periodoAnio;
    }

    public int getPeriodoMes() {
        return periodoMes;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getReporteFinal() {
        return reporteFinal;
    }

    public String getArchivosEntrada() {
        return archivosEntrada;
    }

    public String getRutaArchivoResultado() {
        return rutaArchivoResultado;
    }

    public String getLineasNoEncontradas() {
        return lineasNoEncontradas;
    }

    public String getAvisos() {
        return avisos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReporteGenerado other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}